package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExamSubjectResponse(
        Long id,
        Long examId,
        Long subjectId,
        String subjectName,
        BigDecimal maxMarks,
        BigDecimal passMarks,
        LocalDate examDate,
        LocalTime startTime,
        LocalTime endTime,
        String venue,
        Long invigilatorId,
        String invigilatorName
) {
}
