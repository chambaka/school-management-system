package tz.co.chambaka.school.management.dto.parent;

public record UpdateParentRequest(
        String name,
        String phone,
        String occupation,
        String address,
        Boolean enabled
) {
}
