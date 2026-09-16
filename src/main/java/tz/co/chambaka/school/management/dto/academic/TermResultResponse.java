package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;

public record TermResultResponse(
        Long studentId,
        String studentName,
        Long subjectId,
        String subjectName,
        BigDecimal midterm,
        BigDecimal semiTerminalExam,
        BigDecimal semiTerminalResult,
        BigDecimal terminalExam,
        BigDecimal terminalResult,
        String letterGrade
) {
}
