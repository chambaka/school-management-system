package tz.co.chambaka.school.management.dto.admin;

import tz.co.chambaka.school.management.model.enums.Role;

import java.util.List;

public record UserLinksResponse(
        Long userId,
        String name,
        Role role,
        List<UserLinkItem> links
) {
}
