package tz.co.chambaka.school.management.dto.tenant;

public record OrganizationAdminResponse(
        Long id,
        String name,
        String email,
        String phone,
        boolean enabled
) {
}
