package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.model.BellPeriod;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintEnginesTest {

    @Test
    void timetableConstraints() throws Exception {
        TeacherAvailability window = new TeacherAvailability();
        window.setDayOfWeek(DayOfWeek.MONDAY);
        window.setStartTime(LocalTime.of(8, 0));
        window.setEndTime(LocalTime.of(12, 0));
        assertThat(TimetableConstraintEngine.teacherAvailable(List.of(), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))).isTrue();
        assertThat(TimetableConstraintEngine.teacherAvailable(List.of(window), DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))).isTrue();
        assertThat(TimetableConstraintEngine.teacherAvailable(List.of(window), DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))).isFalse();
        Classroom room = Fixtures.classroom();
        room.setCapacity(20);
        Section section = Fixtures.section();
        section.setCapacity(40);
        assertThat(TimetableConstraintEngine.roomFits(room, section)).isFalse();
        assertThat(TimetableConstraintEngine.roomFits(null, section)).isTrue();
        TimetableSlot slot = new TimetableSlot();
        Subject subject = Fixtures.subject();
        slot.setSubject(subject);
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(8, 40));
        BellPeriod period = new BellPeriod();
        period.setStartTime(LocalTime.of(8, 40));
        period.setEndTime(LocalTime.of(9, 20));
        assertThat(TimetableConstraintEngine.consecutiveSameSubject(List.of(slot), subject.getId(), period)).isTrue();
        assertThat(TimetableConstraintEngine.consecutiveSameSubject(null, 1L, period)).isFalse();
        Constructor<TimetableConstraintEngine> ctor = TimetableConstraintEngine.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }

    @Test
    void examConflicts() throws Exception {
        ExamSubject first = Fixtures.examSubject();
        first.setExamDate(LocalDate.of(2026, 3, 2));
        first.setStartTime(LocalTime.of(8, 0));
        first.setEndTime(LocalTime.of(10, 0));
        first.setVenue("Hall A");
        Teacher invigilator = Fixtures.teacher();
        first.setInvigilator(invigilator);
        ExamSubject clash = Fixtures.examSubject();
        clash.setExamDate(LocalDate.of(2026, 3, 2));
        clash.setStartTime(LocalTime.of(9, 0));
        clash.setEndTime(LocalTime.of(11, 0));
        clash.setVenue("Hall A");
        clash.setInvigilator(invigilator);
        assertThat(ExamConflictEngine.overlaps(first.getExamDate(), first.getStartTime(), first.getEndTime(),
                clash.getExamDate(), clash.getStartTime(), clash.getEndTime())).isTrue();
        assertThat(ExamConflictEngine.invigilatorClash(clash, List.of(first))).isTrue();
        assertThat(ExamConflictEngine.venueClash(clash, List.of(first))).isTrue();
        clash.setInvigilator(null);
        clash.setVenue(" ");
        assertThat(ExamConflictEngine.invigilatorClash(clash, List.of(first))).isFalse();
        assertThat(ExamConflictEngine.venueClash(clash, List.of(first))).isFalse();
        assertThat(ExamConflictEngine.roomTooSmall(Fixtures.classroom(), 80)).isTrue();
        assertThat(ExamConflictEngine.overlaps(null, LocalTime.NOON, LocalTime.NOON, LocalDate.now(), LocalTime.NOON, LocalTime.NOON)).isFalse();
        Constructor<ExamConflictEngine> ctor = ExamConflictEngine.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}
