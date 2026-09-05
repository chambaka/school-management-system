package tz.co.chambaka.school.management.dto.auth;

import tz.co.chambaka.school.management.model.enums.Role;

public record UserProfileResponse(
        Long id,
        Long tenantId,
        Long schoolId,
        Long campusId,
        String name,
        String email,
        Role role,
        String phone,
        String avatarUrl,
        boolean enabled
) {
}
