package tz.co.chambaka.school.management.dto.parent;

import tz.co.chambaka.school.management.model.enums.ParentStatus;

import java.util.List;

public record ParentResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String occupation,
        String address,
        boolean enabled,
        ParentStatus status,
        List<StudentParentResponse> children
) {
}
