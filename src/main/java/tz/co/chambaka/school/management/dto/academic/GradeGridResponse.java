package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.util.List;

public record GradeGridResponse(
        Long examId,
        String examName,
        Long subjectId,
        String subjectName,
        BigDecimal maxMarks,
        BigDecimal passMarks,
        BigDecimal subjectAverage,
        List<Row> rows,
        boolean marksEditable
) {
    public record Row(
            Long studentId,
            String studentName,
            String admissionNo,
            BigDecimal marksObtained,
            String letterGrade,
            boolean passed,
            Integer classPosition
    ) {
    }
}
