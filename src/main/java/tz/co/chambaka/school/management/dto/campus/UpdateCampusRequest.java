package tz.co.chambaka.school.management.dto.campus;

public record UpdateCampusRequest(
        String name,
        String code,
        String address,
        String phone,
        String email,
        String timezone,
        Boolean primaryCampus
) {
}
