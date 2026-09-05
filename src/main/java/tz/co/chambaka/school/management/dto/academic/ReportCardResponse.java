package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.util.List;

public record ReportCardResponse(
        Long studentId,
        String studentName,
        String admissionNo,
        String className,
        String sectionName,
        Long examId,
        String examName,
        List<GradeResponse> subjects,
        BigDecimal totalObtained,
        BigDecimal totalMax,
        BigDecimal percentage,
        String overallGrade
) {
}
