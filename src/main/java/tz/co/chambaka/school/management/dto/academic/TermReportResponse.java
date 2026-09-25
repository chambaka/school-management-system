package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;
import java.util.List;

public record TermReportResponse(
        Long studentId,
        String studentName,
        String admissionNo,
        String className,
        Long academicYearId,
        String academicYearName,
        Long academicTermId,
        String academicTermName,
        List<TermResultResponse> subjects,
        BigDecimal average,
        String overallGrade
) {
}
