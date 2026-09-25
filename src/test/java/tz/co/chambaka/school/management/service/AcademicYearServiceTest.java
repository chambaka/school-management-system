package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.repository.AcademicTermRepository;
import tz.co.chambaka.school.management.repository.AcademicYearRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.FeeStructureRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.ResultWeightConfigRepository;
import tz.co.chambaka.school.management.repository.SchoolClassRepository;
import tz.co.chambaka.school.management.repository.StudentEnrolmentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import tz.co.chambaka.school.management.repository.TimetableLockRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private AcademicMapper academicMapper;
    @Mock
    private AcademicTermRepository academicTermRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private FeeStructureRepository feeStructureRepository;
    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;
    @Mock
    private ResultWeightConfigRepository resultWeightConfigRepository;
    @Mock
    private StudentEnrolmentRepository studentEnrolmentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TimetableSlotRepository timetableSlotRepository;
    @Mock
    private TimetableLockRepository timetableLockRepository;
    @InjectMocks
    private AcademicYearService service;

    @Test
    void listCreateUpdateSetCurrent() {
        AcademicYear year = Fixtures.year();
        AcademicYearResponse dto = new AcademicYearResponse(1L, "2026/2027", year.getStartDate(), year.getEndDate(), true);
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(year));
        when(academicMapper.toYear(any(AcademicYear.class))).thenReturn(dto);
        assertThat(service.list(1L)).hasSize(1);

        when(academicYearRepository.existsBySchoolIdAndNameIgnoreCase(1L, "2026/2027")).thenReturn(false);
        when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(inv -> {
            AcademicYear saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        AcademicYearRequest req = new AcademicYearRequest("2026/2027",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        assertThat(service.create(1L, req).name()).isEqualTo("2026/2027");

        when(academicYearRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(year));
        AcademicYearRequest notCurrent = new AcademicYearRequest("2026/2027",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);
        service.update(1L, 1L, notCurrent);
        service.setCurrent(1L, 1L);
        assertThat(year.isCurrentYear()).isTrue();
    }

    @Test
    void rejectsBadDatesAndDuplicates() {
        AcademicYearRequest bad = new AcademicYearRequest("X",
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), false);
        assertThatThrownBy(() -> service.create(1L, bad)).isInstanceOf(BusinessException.class);
        AcademicYearRequest ok = new AcademicYearRequest("X",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);
        when(academicYearRepository.existsBySchoolIdAndNameIgnoreCase(1L, "X")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, ok)).isInstanceOf(DuplicateResourceException.class);
        when(academicYearRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nextAfterPicksTheFollowingYearThenSchoolCurrent() {
        AcademicYear current = Fixtures.year();
        AcademicYear later = Fixtures.year();
        later.setId(2L);
        later.setName("2027/2028");
        later.setStartDate(java.time.LocalDate.of(2027, 1, 1));
        later.setEndDate(java.time.LocalDate.of(2027, 12, 31));
        later.setCurrentYear(false);
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(later, current));
        assertThat(service.nextAfter(1L, current)).isSameAs(later);

        AcademicYear schoolCurrent = Fixtures.year();
        schoolCurrent.setId(3L);
        schoolCurrent.setName("2028/2029");
        schoolCurrent.setStartDate(java.time.LocalDate.of(2025, 1, 1));
        schoolCurrent.setCurrentYear(true);
        current.setCurrentYear(false);
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(current, schoolCurrent));
        assertThat(service.nextAfter(1L, current)).isSameAs(schoolCurrent);
        assertThat(service.nextAfter(1L, null)).isSameAs(schoolCurrent);
    }

    @Test
    void setCurrentPersistsOnlyTheChosenYear() {
        AcademicYear current = Fixtures.year();
        AcademicYear other = Fixtures.year();
        other.setId(2L);
        other.setName("2027/2028");
        other.setCurrentYear(false);
        when(academicYearRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(other));
        when(academicYearRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(other, current));
        when(academicMapper.toYear(other)).thenReturn(
                new AcademicYearResponse(2L, "2027/2028", other.getStartDate(), other.getEndDate(), true));

        assertThat(service.setCurrent(1L, 2L).currentYear()).isTrue();
        assertThat(current.isCurrentYear()).isFalse();
        assertThat(other.isCurrentYear()).isTrue();
        verify(academicYearRepository).saveAll(List.of(other, current));
    }

    @Test
    void deleteRemovesUnusedYear() {
        AcademicYear year = Fixtures.year();
        when(academicYearRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(year));
        when(academicTermRepository.countByAcademicYearId(1L)).thenReturn(0L);
        when(schoolClassRepository.countByAcademicYearId(1L)).thenReturn(0L);
        when(studentRepository.countByAcademicYearId(1L)).thenReturn(0L);
        when(examRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(feeStructureRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(teacherSubjectRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(resultWeightConfigRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(studentEnrolmentRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(invoiceRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(timetableSlotRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(timetableLockRepository.existsByAcademicYearId(1L)).thenReturn(false);

        service.delete(1L, 1L);

        verify(academicYearRepository).delete(year);
    }

    @Test
    void deleteBlockedWhenYearIsInUse() {
        AcademicYear year = Fixtures.year();
        when(academicYearRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(year));

        when(academicTermRepository.countByAcademicYearId(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("terms");

        when(academicTermRepository.countByAcademicYearId(1L)).thenReturn(0L);
        when(schoolClassRepository.countByAcademicYearId(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("classes");

        when(schoolClassRepository.countByAcademicYearId(1L)).thenReturn(0L);
        when(studentRepository.countByAcademicYearId(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("students");

        when(studentRepository.countByAcademicYearId(1L)).thenReturn(0L);
        when(examRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("exams");

        when(examRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(feeStructureRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("fee");

        when(feeStructureRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(teacherSubjectRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("allocations");

        when(teacherSubjectRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(resultWeightConfigRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("weights");

        when(resultWeightConfigRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(studentEnrolmentRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("enrolment");

        when(studentEnrolmentRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(invoiceRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("invoices");

        when(invoiceRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(timetableSlotRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("timetable");

        when(timetableSlotRepository.existsByAcademicYearId(1L)).thenReturn(false);
        when(timetableLockRepository.existsByAcademicYearId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unlock");
    }
}
