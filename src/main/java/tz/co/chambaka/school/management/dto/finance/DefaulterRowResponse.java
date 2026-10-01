package tz.co.chambaka.school.management.dto.finance;

import java.math.BigDecimal;

public record DefaulterRowResponse(
        String invoiceNumber,
        String studentName,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal discount,
        BigDecimal balance,
        String status
) {
}
