package tz.co.chambaka.school.management.dto.admin;

public record UpdateSchoolAdminRequest(
        String name,
        String phone,
        Boolean enabled
) {
}
