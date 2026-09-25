package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.service.TeacherService;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        when(repository.findBySchoolIdOrderByDayOfWeekAscStartTimeAsc(1L)).thenReturn(List.of(row));
        assertThat(service.list(1L, 1L)).hasSize(1);
        assertThat(service.list(1L, null)).first().extracting("teacherName").isEqualTo(Fixtures.teacher().getUser().getName());

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

        when(repository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(row));
        assertThat(service.update(1L, 1L, new TeacherAvailabilityRequest(
                1L, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(15, 0))).dayOfWeek())
                .isEqualTo(DayOfWeek.WEDNESDAY);
        service.delete(1L, 1L);
        verify(repository).delete(row);
        when(repository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(1L, 1L, new TeacherAvailabilityRequest(
                1L, DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(8, 0))))
                .isInstanceOf(BusinessException.class);

        Teacher unnamed = new Teacher();
        unnamed.setId(8L);
        TeacherAvailability unnamedRow = new TeacherAvailability();
        unnamedRow.setId(3L);
        unnamedRow.setTeacher(unnamed);
        unnamedRow.setDayOfWeek(DayOfWeek.FRIDAY);
        unnamedRow.setStartTime(LocalTime.of(8, 0));
        unnamedRow.setEndTime(LocalTime.of(12, 0));
        when(repository.findBySchoolIdAndTeacherId(1L, 8L)).thenReturn(List.of(unnamedRow));
        assertThat(service.list(1L, 8L)).first().extracting("teacherName").isEqualTo("Teacher");
    }
}
