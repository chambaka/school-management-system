package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.repository.AcademicYearRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private AcademicMapper academicMapper;
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
}
