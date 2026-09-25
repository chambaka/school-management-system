package tz.co.chambaka.school.management.dto.auth;

public record SchoolTwoFactorResponse(
        Long schoolId,
        String schoolName,
        String tenantName,
        boolean enabled
) {
}
