package tz.co.chambaka.school.management.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserProfileResponse user
) {
}
