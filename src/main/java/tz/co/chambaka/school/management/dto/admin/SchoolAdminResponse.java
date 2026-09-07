package tz.co.chambaka.school.management.dto.admin;

import tz.co.chambaka.school.management.model.enums.Role;

public record SchoolAdminResponse(
        Long id,
        String name,
        String email,
        String phone,
        boolean enabled,
        Role role
) {
}
