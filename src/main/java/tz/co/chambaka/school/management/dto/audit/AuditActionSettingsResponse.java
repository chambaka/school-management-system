package tz.co.chambaka.school.management.dto.audit;

import java.util.List;

public record AuditActionSettingsResponse(List<AuditActionSettingResponse> events) {
}
