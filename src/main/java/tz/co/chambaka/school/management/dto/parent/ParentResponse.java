package tz.co.chambaka.school.management.dto.parent;

public record ParentResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String occupation,
        String address,
        boolean enabled
) {
}
