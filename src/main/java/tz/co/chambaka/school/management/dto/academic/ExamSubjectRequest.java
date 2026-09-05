package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExamSubjectRequest(
        @NotNull Long subjectId,
        @NotNull @Positive BigDecimal maxMarks,
        @NotNull @Positive BigDecimal passMarks,
        LocalDate examDate
) {
}
