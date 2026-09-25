package tz.co.chambaka.school.management.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CurriculumTopicRequest(
        @NotNull Long subjectId,
        Long schoolClassId,
        @NotBlank String title,
        String objectives,
        Integer sortOrder
) {
}
