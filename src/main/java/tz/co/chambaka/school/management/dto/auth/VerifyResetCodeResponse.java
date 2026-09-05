package tz.co.chambaka.school.management.dto.auth;

public record VerifyResetCodeResponse(
        String resetToken,
        int expiresInSeconds
) {
}
