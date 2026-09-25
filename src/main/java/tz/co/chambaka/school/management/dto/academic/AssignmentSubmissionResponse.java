package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentSubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String notes,
        String attachmentName,
        BigDecimal marksObtained,
        Instant submittedAt
) {
}
