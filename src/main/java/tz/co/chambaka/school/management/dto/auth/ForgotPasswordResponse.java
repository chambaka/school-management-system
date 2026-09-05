package tz.co.chambaka.school.management.dto.auth;

public record ForgotPasswordResponse(
        String message,
        String maskedEmail,
        int expiresInSeconds,
        String debugCode
) {
}
