package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeRequest(
        @NotNull Long examId,
        @NotNull Long studentId,
        @NotNull Long subjectId,
        @NotNull BigDecimal marksObtained,
        String remarks
) {
}
