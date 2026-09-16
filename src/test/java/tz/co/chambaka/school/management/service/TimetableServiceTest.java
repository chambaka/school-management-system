package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.TimetableRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.model.BellPeriod;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.model.TimetableLock;
import tz.co.chambaka.school.management.model.enums.PeriodKind;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import tz.co.chambaka.school.management.repository.TimetableLockRepository;
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
    @Mock
    private TimetableLockRepository timetableLockRepository;
    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private BellPeriodService bellPeriodService;
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

    @Test
    void generatesTimetableFromAllocationsAndLocksIt() {
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        when(timetableSlotRepository.findBySchoolIdAndSectionIdOrderByDayOfWeekAscStartTimeAsc(1L, 1L))
                .thenReturn(List.of(slot(LocalTime.of(7, 0), LocalTime.of(8, 0))));
        when(teacherSubjectRepository.findBySchoolId(1L)).thenReturn(List.of(allocation()));
        when(bellPeriodService.lessonPeriods(1L)).thenReturn(List.of(period()));
        when(classroomRepository.findBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(Fixtures.classroom()));
        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenAnswer(invocation -> {
            TimetableSlot saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        assertThat(service.generate(1L, 1L, 1L)).singleElement()
                .satisfies(row -> assertThat(row.room()).isEqualTo("Lab 1"));

        when(timetableLockRepository.findBySchoolIdAndSectionIdAndAcademicYearId(1L, 1L, 1L))
                .thenReturn(Optional.empty());
        assertThat(service.lock(1L, 1L, 1L, true)).isTrue();
        verify(timetableLockRepository).save(any(TimetableLock.class));
    }

    @Test
    void reportsAndEnforcesStoredLock() {
        TimetableLock lock = new TimetableLock();
        lock.setLocked(true);
        when(timetableLockRepository.findBySchoolIdAndSectionIdAndAcademicYearId(1L, 1L, 1L))
                .thenReturn(Optional.of(lock));
        assertThat(service.isLocked(1L, 1L, 1L)).isTrue();
        assertThatThrownBy(() -> service.generate(1L, 1L, 1L)).isInstanceOf(BusinessException.class);
        assertThat(service.isLocked(1L, 1L, null)).isFalse();
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

    private TeacherSubject allocation() {
        TeacherSubject allocation = new TeacherSubject();
        allocation.setAcademicYear(Fixtures.year());
        allocation.setSchoolClass(Fixtures.schoolClass());
        allocation.setSection(Fixtures.section());
        allocation.setSubject(Fixtures.subject());
        allocation.setTeacher(Fixtures.teacher());
        allocation.setWeeklyLessons(1);
        return allocation;
    }

    private BellPeriod period() {
        BellPeriod period = new BellPeriod();
        period.setStartTime(LocalTime.of(8, 0));
        period.setEndTime(LocalTime.of(9, 0));
        period.setKind(PeriodKind.LESSON);
        return period;
    }
}
