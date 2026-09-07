package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.TimetableRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class TimetableServiceTest {

    @Mock
    private TimetableSlotRepository timetableSlotRepository;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private SectionService sectionService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private TeacherService teacherService;
    @InjectMocks
    private TimetableService service;

    @Test
    void listCreateDelete() {
        TimetableSlot slot = slot(LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(timetableSlotRepository.findBySchoolIdAndSectionIdOrderByDayOfWeekAscStartTimeAsc(1L, 1L))
                .thenReturn(List.of(slot));
        when(timetableSlotRepository.findBySchoolIdAndTeacherIdOrderByDayOfWeekAscStartTimeAsc(1L, 1L))
                .thenReturn(List.of(slot));
        assertThat(service.bySection(1L, 1L)).hasSize(1);
        assertThat(service.byTeacher(1L, 1L).getFirst().room()).isEqualTo("R1");
        assertThat(service.byTeacher(1L, 1L).getFirst().schoolClassName()).isEqualTo("Form 1");
        assertThat(service.byTeacher(1L, 1L).getFirst().sectionName()).isEqualTo("A");

        when(timetableSlotRepository.findBySectionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenAnswer(inv -> {
            TimetableSlot saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        TimetableRequest req = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), "Lab");
        assertThat(service.create(1L, req).id()).isEqualTo(2L);

        when(timetableSlotRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(slot));
        TimetableRequest update = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.TUESDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Lab 2");
        assertThat(service.update(1L, 1L, update).room()).isEqualTo("Lab 2");
        assertThat(slot.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);

        when(timetableSlotRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(slot));
        service.delete(1L, 2L);
        verify(timetableSlotRepository).delete(slot);
    }

    @Test
    void rejectsBadTimeAndClashes() {
        TimetableRequest bad = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(11, 0), LocalTime.of(10, 0), null);
        assertThatThrownBy(() -> service.create(1L, bad)).isInstanceOf(BusinessException.class)
                .hasMessage("End time must be after start time");

        when(timetableSlotRepository.findBySectionIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slot(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        TimetableRequest sectionClash = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 30), LocalTime.of(9, 30), null);
        assertThatThrownBy(() -> service.create(1L, sectionClash))
                .isInstanceOf(BusinessException.class)
                .hasMessage("This slot overlaps an existing timetable entry");

        when(timetableSlotRepository.findBySectionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(timetableSlotRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(slot(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        TimetableRequest teacherClash = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 15), LocalTime.of(8, 45), null);
        assertThatThrownBy(() -> service.create(1L, teacherClash))
                .isInstanceOf(BusinessException.class)
                .hasMessage("This teacher is already teaching at that time");

        when(timetableSlotRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(timetableSlotRepository.findBySchoolIdAndRoomIgnoreCaseAndDayOfWeek(1L, "Lab", DayOfWeek.MONDAY))
                .thenReturn(List.of(slot(LocalTime.of(8, 0), LocalTime.of(9, 0))));
        TimetableRequest roomClash = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "Lab");
        assertThatThrownBy(() -> service.create(1L, roomClash))
                .isInstanceOf(BusinessException.class)
                .hasMessage("This room is already booked at that time");

        when(timetableSlotRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(1L, 9L, sectionClash)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateIgnoresOwnSlotAndBlankRoom() {
        TimetableSlot own = slot(LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(timetableSlotRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(own));
        when(timetableSlotRepository.findBySectionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(List.of(own));
        when(timetableSlotRepository.findByTeacherIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(List.of(own));
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        TimetableRequest same = new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "  ");
        assertThat(service.update(1L, 1L, same).room()).isNull();
    }

    @Test
    void mapsSlotWithoutClass() {
        TimetableSlot slot = slot(LocalTime.of(8, 0), LocalTime.of(9, 0));
        slot.getSection().setSchoolClass(null);
        when(timetableSlotRepository.findBySchoolIdAndSectionIdOrderByDayOfWeekAscStartTimeAsc(1L, 1L))
                .thenReturn(List.of(slot));
        assertThat(service.bySection(1L, 1L).getFirst().schoolClassName()).isNull();
        assertThat(service.bySection(1L, 1L).getFirst().schoolClassId()).isNull();
    }

    private TimetableSlot slot(LocalTime start, LocalTime end) {
        TimetableSlot slot = new TimetableSlot();
        slot.setId(1L);
        slot.setSection(Fixtures.section());
        slot.setSubject(Fixtures.subject());
        slot.setTeacher(Fixtures.teacher());
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setRoom("R1");
        return slot;
    }
}
