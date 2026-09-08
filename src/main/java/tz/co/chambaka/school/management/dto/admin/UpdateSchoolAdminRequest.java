package tz.co.chambaka.school.management.dto.admin;

import tz.co.chambaka.school.management.model.enums.Role;

public record UpdateSchoolAdminRequest(
        String name,
        String phone,
        Boolean enabled,
        Role role
) {
}
