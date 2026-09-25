package tz.co.chambaka.school.management.dto.auth;

import java.util.List;

public record TwoFactorSettingsResponse(
        String name,
        String description,
        List<SchoolTwoFactorResponse> schools
) {
}
