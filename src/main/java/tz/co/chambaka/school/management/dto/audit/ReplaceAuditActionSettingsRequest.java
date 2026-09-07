package tz.co.chambaka.school.management.dto.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceAuditActionSettingsRequest(
        @NotEmpty @Valid List<Item> events
) {
    public record Item(@NotNull AuditAction action, @NotNull Boolean enabled) {
    }
}
