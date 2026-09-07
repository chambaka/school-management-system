package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.Gender;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private ClassService classService;
    @Mock
    private SectionService sectionService;
    @InjectMocks
    private StudentService service;

    @Test
    void listGetCreateUpdate() {
        Student student = Fixtures.student();
        when(studentRepository.findBySchoolId(1L, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(student)));
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(student)));
        assertThat(service.list(1L, null, PageRequest.of(0, 5)).content()).hasSize(1);
        assertThat(service.list(1L, 1L, PageRequest.of(0, 5)).content().getFirst().sectionName()).isEqualTo("A");

        when(studentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(student));
        assertThat(service.get(1L, 1L).admissionNo()).isEqualTo("ADM-001");

        when(studentRepository.countBySchoolIdAndAdmissionNoStartingWithIgnoreCase(any(), any())).thenReturn(1L);
        when(studentRepository.existsBySchoolIdAndAdmissionNoIgnoreCase(any(), any())).thenReturn(false);
        when(userAccountService.create(1L, "Juma", "j@x.com", "pw", Role.STUDENT, "07"))
                .thenReturn(Fixtures.user(4L, Role.STUDENT));
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        CreateStudentRequest create = new CreateStudentRequest(
                "Juma", "j@x.com", "pw", "07", "1", LocalDate.of(2010, 1, 1),
                Gender.MALE, "O+", LocalDate.of(2026, 1, 1), "addr", "0711", 1L, 1L, 1L);
        StudentResponse created = service.create(1L, create);
        assertThat(created.id()).isEqualTo(2L);
        assertThat(created.admissionNo()).startsWith("ADM-");

        service.update(1L, 1L, new UpdateStudentRequest(
                "Juma 2", "08", "2", LocalDate.of(2011, 1, 1), Gender.FEMALE, "A+",
                "new", "0722", 1L, 1L, 1L, false));
        assertThat(student.getUser().getName()).isEqualTo("Juma 2");
        assertThat(student.getUser().isEnabled()).isFalse();
        assertThat(student.getGender()).isEqualTo(Gender.FEMALE);
    }

    @Test
    void createWithoutPlacementAndErrors() {
        when(studentRepository.countBySchoolIdAndAdmissionNoStartingWithIgnoreCase(any(), any())).thenReturn(0L);
        when(studentRepository.existsBySchoolIdAndAdmissionNoIgnoreCase(any(), any())).thenReturn(true, false);
        when(userAccountService.create(1L, "N", "n@x.com", "pw", Role.STUDENT, null))
                .thenReturn(Fixtures.user(4L, Role.STUDENT));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateStudentRequest bare = new CreateStudentRequest(
                "N", "n@x.com", "pw", null, null, null, null, null, null, null, null, null, null, null);
        assertThat(service.create(1L, bare).schoolClassId()).isNull();
        assertThat(service.create(1L, bare).admissionNo()).startsWith("ADM-");

        when(studentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(studentRepository.findByUserId(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireByUser(8L)).isInstanceOf(ResourceNotFoundException.class);
        when(studentRepository.findByUserId(4L)).thenReturn(Optional.of(Fixtures.student()));
        assertThat(service.requireByUser(4L).getAdmissionNo()).isEqualTo("ADM-001");
    }
}
