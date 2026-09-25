package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;

public record MeritRowResponse(
        int position,
        Long studentId,
        String studentName,
        String admissionNo,
        BigDecimal total,
        BigDecimal percentage,
        String grade
) {
}
