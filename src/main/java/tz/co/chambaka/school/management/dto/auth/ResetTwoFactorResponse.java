package tz.co.chambaka.school.management.dto.auth;

public record ResetTwoFactorResponse(
        Long userId,
        String name,
        String email,
        boolean totpEnabled
) {
}
