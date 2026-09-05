package tz.co.chambaka.school.management.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank @Size(min = 10, max = 72) String newPassword
) {
}
