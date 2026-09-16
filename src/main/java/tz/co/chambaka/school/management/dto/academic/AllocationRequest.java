package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;

public record AllocationRequest(
        @NotNull Long teacherId,
        @NotNull Long subjectId,
        @NotNull Long schoolClassId,
        Long sectionId,
        @NotNull Long academicYearId,
        Integer weeklyLessons
) {
    public AllocationRequest(Long teacherId, Long subjectId, Long schoolClassId, Long sectionId, Long academicYearId) {
        this(teacherId, subjectId, schoolClassId, sectionId, academicYearId, 5);
    }
}
