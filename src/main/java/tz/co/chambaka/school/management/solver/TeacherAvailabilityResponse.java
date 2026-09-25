package tz.co.chambaka.school.management.solver;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TeacherAvailabilityResponse(
        Long id,
        Long teacherId,
        String teacherName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
