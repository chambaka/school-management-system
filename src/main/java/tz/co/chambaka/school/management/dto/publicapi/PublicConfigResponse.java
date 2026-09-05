package tz.co.chambaka.school.management.dto.publicapi;

public record PublicConfigResponse(
        String tenancyMode,
        boolean registrationEnabled,
        String organizationName
) {
}
