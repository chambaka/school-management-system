package tz.co.chambaka.school.management.dto.admin;

public record SetUserEnabledResponse(
        Long userId,
        String name,
        String email,
        boolean enabled
) {
}
