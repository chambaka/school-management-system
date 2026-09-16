package tz.co.chambaka.school.management.dto.academic;

import java.time.Instant;
import java.time.LocalDate;

public record AssignmentResponse(
        Long id,
        Long teacherId,
        String teacherName,
        Long schoolClassId,
        String schoolClassName,
        Long sectionId,
        String sectionName,
        Long subjectId,
        String subjectName,
        String title,
        String instructions,
        LocalDate dueDate,
        String attachmentName,
        Instant publishedAt
) {
}
