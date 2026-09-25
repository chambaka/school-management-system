package tz.co.chambaka.school.management.dto.auth;

import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @Size(min = 10, max = 128) String password
) {
}
