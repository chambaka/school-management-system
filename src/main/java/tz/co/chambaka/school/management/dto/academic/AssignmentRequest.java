package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignmentRequest(
        @NotNull Long schoolClassId,
        Long sectionId,
        @NotNull Long subjectId,
        @NotBlank String title,
        String instructions,
        @NotNull LocalDate dueDate
) {
}
