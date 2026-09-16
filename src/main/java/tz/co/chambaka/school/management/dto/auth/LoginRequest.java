package tz.co.chambaka.school.management.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String email,
        String identifier,
        @NotBlank String password
) {
    public LoginRequest(String email, String password) {
        this(email, null, password);
    }

    public String loginKey() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        return email == null ? "" : email.trim();
    }
}
