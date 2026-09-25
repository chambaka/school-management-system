package tz.co.chambaka.school.management.dto.auth;

public record AdminResetPasswordResponse(
        Long userId,
        String name,
        String email,
        boolean smsSent
) {
}
