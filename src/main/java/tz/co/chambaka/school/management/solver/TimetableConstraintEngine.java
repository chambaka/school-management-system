package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.model.BellPeriod;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.TimetableSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public final class TimetableConstraintEngine {

    private TimetableConstraintEngine() {
    }

    public static boolean teacherAvailable(
            List<TeacherAvailability> windows,
            DayOfWeek day,
            LocalTime start,
            LocalTime end
    ) {
        if (windows == null || windows.isEmpty()) {
            return true;
        }
        return windows.stream()
                .filter(window -> window.getDayOfWeek() == day)
                .anyMatch(window -> !window.getStartTime().isAfter(start) && !window.getEndTime().isBefore(end));
    }

    public static boolean roomFits(Classroom room, Section section) {
        if (room == null || room.getCapacity() == null || section == null || section.getCapacity() == null) {
            return true;
        }
        return room.getCapacity() >= section.getCapacity();
    }

    public static boolean consecutiveSameSubject(List<TimetableSlot> daySlots, Long subjectId, BellPeriod period) {
        if (subjectId == null || period == null || daySlots == null) {
            return false;
        }
        return daySlots.stream().anyMatch(slot ->
                subjectId.equals(slot.getSubject() == null ? null : slot.getSubject().getId())
                        && (slot.getEndTime().equals(period.getStartTime())
                        || period.getEndTime().equals(slot.getStartTime())));
    }
}
