package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.PromotionAction;

import java.time.LocalDate;

public record EnrolmentHistoryResponse(
        Long id,
        Long studentId,
        String studentName,
        String admissionNo,
        PromotionAction action,
        LocalDate effectiveDate,
        Long academicYearId,
        String academicYearName,
        Long schoolClassId,
        String schoolClassName,
        Long sectionId,
        String sectionName,
        String notes
) {
}
