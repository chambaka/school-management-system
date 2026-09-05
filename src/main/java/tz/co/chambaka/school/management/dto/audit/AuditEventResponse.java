package tz.co.chambaka.school.management.dto.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        String correctionId,
        AuditScope scope,
        Long schoolId,
        Long actorUserId,
        String actorEmail,
        String actorRole,
        AuditAction action,
        String resourceType,
        String resourceId,
        String summary,
        String details,
        String httpMethod,
        String httpPath,
        Integer statusCode,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
