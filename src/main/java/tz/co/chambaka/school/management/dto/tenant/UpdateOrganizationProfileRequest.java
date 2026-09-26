package tz.co.chambaka.school.management.dto.tenant;

public record UpdateOrganizationProfileRequest(
        String email,
        String phone,
        String country,
        String timezone,
        String currency,
        Integer termsPerYear
) {
}
