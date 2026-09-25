package tz.co.chambaka.school.management.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        Long studentId,
        Long invoiceId,
        Long paymentId,
        LedgerEntryType entryType,
        BigDecimal amount,
        String description,
        Instant occurredAt
) {
}
