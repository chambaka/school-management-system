package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ResultWeightRequest(
        @NotNull Long academicYearId,
        Long academicTermId,
        Long subjectId,
        @NotNull BigDecimal midtermWeight,
        @NotNull BigDecimal semiExamWeight,
        @NotNull BigDecimal semiResultWeight,
        @NotNull BigDecimal terminalExamWeight
) {
}
