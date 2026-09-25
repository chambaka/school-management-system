package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.academic.BellPeriodRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.BellPeriod;
import tz.co.chambaka.school.management.model.enums.PeriodKind;
import tz.co.chambaka.school.management.repository.BellPeriodRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BellPeriodServiceTest {

    @Mock BellPeriodRepository repository;
    @InjectMocks BellPeriodService service;

    @Test
    void suppliesDefaultsAndDefaultLessonEntities() {
        when(repository.findBySchoolIdOrderBySortOrderAsc(1L)).thenReturn(List.of());
        assertThat(service.list(1L)).hasSize(10);
        assertThat(service.lessonPeriods(1L)).hasSize(8)
                .allSatisfy(period -> assertThat(period.getKind()).isEqualTo(PeriodKind.LESSON));
    }

    @Test
    void createsListsAndFiltersStoredPeriods() {
        BellPeriod lesson = period(1L, "P1", PeriodKind.LESSON);
        BellPeriod lunch = period(2L, "Lunch", PeriodKind.BREAK);
        when(repository.findBySchoolIdOrderBySortOrderAsc(1L)).thenReturn(List.of(lesson, lunch));
        assertThat(service.list(1L)).hasSize(2);
        assertThat(service.lessonPeriods(1L)).containsExactly(lesson);

        when(repository.save(any(BellPeriod.class))).thenAnswer(invocation -> {
            BellPeriod saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        var response = service.create(1L, new BellPeriodRequest(
                "P2", LocalTime.of(8, 10), LocalTime.of(8, 50), PeriodKind.LESSON, 2));
        assertThat(response.id()).isEqualTo(3L);
    }

    @Test
    void rejectsInvalidTimeAndDeletesOnlyExistingPeriod() {
        assertThatThrownBy(() -> service.create(1L, new BellPeriodRequest(
                "Bad", LocalTime.NOON, LocalTime.NOON, PeriodKind.LESSON, 1)))
                .isInstanceOf(BusinessException.class);

        BellPeriod period = period(1L, "P1", PeriodKind.LESSON);
        when(repository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(period));
        assertThat(service.update(1L, 1L, new BellPeriodRequest(
                "Period 1", LocalTime.of(7, 30), LocalTime.of(8, 10), PeriodKind.LESSON, 1)).name())
                .isEqualTo("Period 1");
        assertThatThrownBy(() -> service.update(1L, 1L, new BellPeriodRequest(
                "Bad", LocalTime.NOON, LocalTime.NOON, PeriodKind.LESSON, 1)))
                .isInstanceOf(BusinessException.class);
        service.delete(1L, 1L);
        verify(repository).delete(period);

        when(repository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private BellPeriod period(Long id, String name, PeriodKind kind) {
        BellPeriod period = new BellPeriod();
        period.setId(id);
        period.setName(name);
        period.setStartTime(LocalTime.of(8, 0));
        period.setEndTime(LocalTime.of(9, 0));
        period.setKind(kind);
        period.setSortOrder(id.intValue());
        return period;
    }
}
