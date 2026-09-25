package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExamSubjectRequest(
        @NotNull Long subjectId,
        @NotNull @Positive BigDecimal maxMarks,
        @NotNull @Positive BigDecimal passMarks,
        LocalDate examDate,
        LocalTime startTime,
        LocalTime endTime,
        String venue,
        @NotNull Long invigilatorId
) {
    public ExamSubjectRequest(Long subjectId, BigDecimal maxMarks, BigDecimal passMarks, LocalDate examDate) {
        this(subjectId, maxMarks, passMarks, examDate, null, null, null, null);
    }
}
