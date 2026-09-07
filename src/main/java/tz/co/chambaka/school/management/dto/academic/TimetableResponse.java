package tz.co.chambaka.school.management.dto.academic;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableResponse(
        Long id,
        Long academicYearId,
        Long schoolClassId,
        String schoolClassName,
        Long sectionId,
        String sectionName,
        Long subjectId,
        String subjectName,
        Long teacherId,
        String teacherName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String room
) {
}
