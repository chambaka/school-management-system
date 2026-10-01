package tz.co.chambaka.school.management.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record CollectionRowResponse(
        String receiptNumber,
        String studentName,
        BigDecimal amount,
        String method,
        Instant paidAt
) {
}
