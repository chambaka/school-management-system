package tz.co.chambaka.school.management.dto.audit;

import jakarta.validation.constraints.NotNull;

public record UpdateAuditActionSettingRequest(@NotNull Boolean enabled) {
}
