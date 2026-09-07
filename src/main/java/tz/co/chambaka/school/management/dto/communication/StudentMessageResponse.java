package tz.co.chambaka.school.management.dto.communication;

import tz.co.chambaka.school.management.model.enums.Role;

import java.time.Instant;
import java.util.List;

public record StudentMessageResponse(
        Long id,
        Long studentId,
        String studentName,
        Long authorUserId,
        String authorName,
        Role authorRole,
        String body,
        boolean notifyParentsSms,
        int smsSent,
        int smsFailed,
        int smsSkipped,
        Instant createdAt,
        List<SmsDeliveryResponse> smsDeliveries
) {
}
