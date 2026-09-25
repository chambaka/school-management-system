package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.student.PromoteStudentsRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentEnrolment;
import tz.co.chambaka.school.management.model.enums.PromotionAction;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.StudentEnrolmentRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock StudentService studentService;
    @Mock AcademicYearService academicYearService;
    @Mock ClassService classService;
    @Mock SectionService sectionService;
    @Mock StudentEnrolmentRepository enrolmentRepository;
    @InjectMocks PromotionService service;

    @Test
    void promotionRequiresDestinationClass() {
        assertThatThrownBy(() -> service.promote(1L, request(PromotionAction.PROMOTE, null, null, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.preview(1L, request(PromotionAction.PROMOTE, null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void previewShowsFromAndToPlacement() {
        Student student = stubStudent();
        when(classService.require(1L, 2L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 2L)).thenReturn(Fixtures.section());
        when(academicYearService.require(1L, 2L)).thenReturn(Fixtures.year());
        var rows = service.preview(1L, request(PromotionAction.PROMOTE, 2L, 2L, 2L));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().studentId()).isEqualTo(student.getId());
        assertThat(rows.getFirst().action()).isEqualTo(PromotionAction.PROMOTE);
        assertThat(rows.getFirst().toClass()).isEqualTo("Form 1");
    }

    @Test
    void promotesAndChangesYearClassAndSection() {
        Student student = stubStudent();
        var nextClass = Fixtures.schoolClass();
        nextClass.setId(2L);
        var nextSection = Fixtures.section();
        nextSection.setId(2L);
        when(academicYearService.require(1L, 2L)).thenReturn(Fixtures.year());
        when(classService.require(1L, 2L)).thenReturn(nextClass);
        when(sectionService.require(1L, 2L)).thenReturn(nextSection);

        service.promote(1L, request(PromotionAction.PROMOTE, 2L, 2L, 2L));

        assertThat(student.getSchoolClass()).isSameAs(nextClass);
        assertThat(student.getSection()).isSameAs(nextSection);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(student.getUser().isEnabled()).isTrue();
        verify(enrolmentRepository).save(any(StudentEnrolment.class));
    }

    @Test
    void repeatAdvancesToNextYearWhenNonePicked() {
        Student student = stubStudent();
        AcademicYear next = Fixtures.year();
        next.setId(2L);
        next.setName("2027/2028");
        when(academicYearService.nextAfter(1L, student.getAcademicYear())).thenReturn(next);

        service.promote(1L, request(PromotionAction.REPEAT, null, null, null));

        assertThat(student.getAcademicYear()).isSameAs(next);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(student.getUser().isEnabled()).isTrue();
    }

    @Test
    void repeatUsesPickedYear() {
        Student student = stubStudent();
        AcademicYear picked = Fixtures.year();
        picked.setId(3L);
        when(academicYearService.require(1L, 3L)).thenReturn(picked);
        service.promote(1L, request(PromotionAction.REPEAT, 3L, null, null));
        assertThat(student.getAcademicYear()).isSameAs(picked);
    }

    @Test
    void listsEnrolmentHistoryForStudentAndSchool() {
        Student student = stubStudent();
        StudentEnrolment row = new StudentEnrolment();
        row.setId(9L);
        row.setStudent(student);
        row.setAcademicYear(student.getAcademicYear());
        row.setSchoolClass(student.getSchoolClass());
        row.setSection(student.getSection());
        row.setAction(PromotionAction.REPEAT);
        row.setEffectiveDate(java.time.LocalDate.of(2026, 1, 15));
        row.setNotes("Repeat year");
        when(enrolmentRepository.search(1L, 1L, null, null)).thenReturn(List.of(row));
        when(enrolmentRepository.search(1L, null, 1L, PromotionAction.PROMOTE)).thenReturn(List.of());

        var history = service.history(1L, 1L, null, null);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).admissionNo()).isEqualTo("ADM-001");
        assertThat(history.get(0).action()).isEqualTo(PromotionAction.REPEAT);
        assertThat(history.get(0).academicYearName()).isEqualTo("2026/2027");
        assertThat(service.history(1L, null, 1L, PromotionAction.PROMOTE)).isEmpty();
    }

    @Test
    void graduatesAndTransfersStudents() {
        stubStudent();
        service.promote(1L, request(PromotionAction.GRADUATE, null, null, null));
        verify(studentService).setLifecycle(1L, 1L, StudentStatus.GRADUATED);

        service.promote(1L, request(PromotionAction.TRANSFER, null, null, null));
        verify(studentService).setLifecycle(1L, 1L, StudentStatus.TRANSFERRED);
    }

    private Student stubStudent() {
        Student student = Fixtures.student();
        when(studentService.require(1L, 1L)).thenReturn(student);
        return student;
    }

    private PromoteStudentsRequest request(PromotionAction action, Long year, Long schoolClass, Long section) {
        return new PromoteStudentsRequest(List.of(1L), action, year, schoolClass, section, "Year end");
    }
}
