package tz.co.chambaka.school.management.dto.admin;

public record SchoolAdminResponse(
        Long id,
        String name,
        String email,
        String phone,
        boolean enabled
) {
}
