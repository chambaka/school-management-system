package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.service.TeacherService;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAvailabilityServiceTest {

    @Mock TeacherAvailabilityRepository repository;
    @Mock TeacherService teacherService;
    @InjectMocks TeacherAvailabilityService service;

    @Test
    void listsAndCreatesWindows() {
        TeacherAvailability row = new TeacherAvailability();
        row.setId(1L);
        row.setTeacher(Fixtures.teacher());
        row.setDayOfWeek(DayOfWeek.MONDAY);
        row.setStartTime(LocalTime.of(8, 0));
        row.setEndTime(LocalTime.of(16, 0));
        when(repository.findBySchoolIdAndTeacherId(1L, 1L)).thenReturn(List.of(row));
        assertThat(service.list(1L, 1L)).hasSize(1);

        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        when(repository.save(any(TeacherAvailability.class))).thenAnswer(inv -> {
            TeacherAvailability saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(service.create(1L, new TeacherAvailabilityRequest(
                1L, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))).id()).isEqualTo(2L);
        assertThatThrownBy(() -> service.create(1L, new TeacherAvailabilityRequest(
                1L, DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(8, 0))))
                .isInstanceOf(BusinessException.class);
    }
}
