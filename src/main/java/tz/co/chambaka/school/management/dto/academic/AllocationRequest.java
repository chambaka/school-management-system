package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;

public record AllocationRequest(
        @NotNull Long teacherId,
        @NotNull Long subjectId,
        @NotNull Long schoolClassId,
        Long sectionId,
        @NotNull Long academicYearId
) {
}
