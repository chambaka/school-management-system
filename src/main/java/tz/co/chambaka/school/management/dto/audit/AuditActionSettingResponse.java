package tz.co.chambaka.school.management.dto.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;

public record AuditActionSettingResponse(
        AuditAction action,
        String group,
        String label,
        String description,
        boolean enabled
) {
}
