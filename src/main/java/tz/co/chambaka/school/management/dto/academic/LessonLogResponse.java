package tz.co.chambaka.school.management.dto.academic;

import java.time.LocalDate;

public record LessonLogResponse(
        Long id,
        Long timetableSlotId,
        Long teacherId,
        String teacherName,
        Long sectionId,
        String sectionName,
        Long subjectId,
        String subjectName,
        LocalDate lessonDate,
        String topic,
        String objectives,
        String materials,
        String homework
) {
}
