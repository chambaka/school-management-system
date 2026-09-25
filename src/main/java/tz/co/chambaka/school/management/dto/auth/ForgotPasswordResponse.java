package tz.co.chambaka.school.management.dto.auth;

public record ForgotPasswordResponse(
        String message,
        String maskedPhone,
        int expiresInSeconds,
        String debugCode
) {
}
