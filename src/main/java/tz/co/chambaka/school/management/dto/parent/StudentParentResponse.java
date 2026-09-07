package tz.co.chambaka.school.management.dto.parent;

import tz.co.chambaka.school.management.model.enums.RelationshipType;

public record StudentParentResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentAdmissionNo,
        Long parentId,
        String parentName,
        RelationshipType relationship,
        boolean primaryContact
) {
}
