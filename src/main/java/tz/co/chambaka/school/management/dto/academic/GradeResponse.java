package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;

public record GradeResponse(
        Long id,
        Long examId,
        Long studentId,
        String studentName,
        Long subjectId,
        String subjectName,
        BigDecimal marksObtained,
        BigDecimal maxMarks,
        BigDecimal passMarks,
        boolean passed,
        String remarks
) {
}
