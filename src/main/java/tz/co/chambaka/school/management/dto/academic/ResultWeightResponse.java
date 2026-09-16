package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;

public record ResultWeightResponse(
        Long id,
        Long academicYearId,
        Long academicTermId,
        Long subjectId,
        String subjectName,
        BigDecimal midtermWeight,
        BigDecimal semiExamWeight,
        BigDecimal semiResultWeight,
        BigDecimal terminalExamWeight
) {
}
