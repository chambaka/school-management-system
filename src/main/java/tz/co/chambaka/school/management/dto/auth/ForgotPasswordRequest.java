package tz.co.chambaka.school.management.dto.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank
        @JsonAlias({"email", "phone"})
        String identifier
) {
}
