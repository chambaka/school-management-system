package tz.co.chambaka.school.management.dto.admin;

public record RemoveUserLinkRequest(
        String unlinkKind,
        Long id,
        Long studentId,
        Long parentId
) {
}
