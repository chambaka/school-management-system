package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.PromotionAction;

public record PromotionPreviewResponse(
        Long studentId,
        String studentName,
        String admissionNo,
        PromotionAction action,
        String fromYear,
        String fromClass,
        String fromSection,
        String toYear,
        String toClass,
        String toSection
) {
}
