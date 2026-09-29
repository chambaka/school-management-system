package tz.co.chambaka.school.management.dto.admin;

public record UserLinkItem(
        String type,
        Long id,
        String title,
        String detail,
        boolean removable,
        String unlinkKind,
        Long studentId,
        Long parentId
) {
    public static UserLinkItem info(String type, Long id, String title, String detail) {
        return new UserLinkItem(type, id, title, detail, false, null, null, null);
    }

    public static UserLinkItem allocation(Long id, String title, String detail) {
        return new UserLinkItem("Allocation", id, title, detail, true, "ALLOCATION", null, null);
    }

    public static UserLinkItem studentParent(
            String type, Long id, String title, String detail, Long studentId, Long parentId
    ) {
        return new UserLinkItem(type, id, title, detail, true, "STUDENT_PARENT", studentId, parentId);
    }
}
