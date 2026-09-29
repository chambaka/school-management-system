package tz.co.chambaka.school.management.dto.admin;

public record RemoveUserResponse(
        Long userId,
        String name,
        String email,
        boolean deleted,
        boolean disabled
) {
}
