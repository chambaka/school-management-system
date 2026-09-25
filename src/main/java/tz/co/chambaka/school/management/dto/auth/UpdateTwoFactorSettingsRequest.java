package tz.co.chambaka.school.management.dto.auth;

import jakarta.validation.constraints.NotNull;

public record UpdateTwoFactorSettingsRequest(@NotNull Boolean enabled) {
}
