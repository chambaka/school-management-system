package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AssignmentSubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String notes,
        String attachmentName,
        BigDecimal marksObtained,
        Instant submittedAt,
        Integer attempt,
        List<AssignmentAttachmentResponse> attachments
) {
}
