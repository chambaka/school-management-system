package tz.co.chambaka.school.management.dto.admin;

import tz.co.chambaka.school.management.model.enums.Role;

import java.time.Instant;

public record SchoolUserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        boolean enabled,
        Instant lockedUntil,
        boolean totpEnabled,
        Instant lastLoginAt,
        Long schoolId,
        String schoolName,
        Long tenantId,
        String tenantName
) {
}
