package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.PromotionAction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PromoteStudentsRequest(
        @NotEmpty List<Long> studentIds,
        @NotNull PromotionAction action,
        Long academicYearId,
        Long schoolClassId,
        Long sectionId,
        String notes
) {
}
