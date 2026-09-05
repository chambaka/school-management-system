package tz.co.chambaka.school.management.dto.campus;

public record CampusResponse(
        Long id,
        Long tenantId,
        Long schoolId,
        String name,
        String code,
        String address,
        String phone,
        String email,
        String timezone,
        boolean primaryCampus
) {
}
