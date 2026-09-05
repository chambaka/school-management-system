package tz.co.chambaka.school.management.dto.finance;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        String description,
        BigDecimal amount
) {
}
