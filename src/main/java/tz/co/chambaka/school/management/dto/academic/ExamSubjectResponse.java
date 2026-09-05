package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExamSubjectResponse(
        Long id,
        Long examId,
        Long subjectId,
        String subjectName,
        BigDecimal maxMarks,
        BigDecimal passMarks,
        LocalDate examDate
) {
}
