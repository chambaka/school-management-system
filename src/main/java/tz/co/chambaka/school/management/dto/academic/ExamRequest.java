package tz.co.chambaka.school.management.dto.academic;

import tz.co.chambaka.school.management.model.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExamRequest(
        @NotNull Long academicYearId,
        @NotNull Long schoolClassId,
        @NotBlank String name,
        @NotNull ExamType examType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
