package tz.co.chambaka.school.management.dto.finance;

import java.math.BigDecimal;
import java.util.List;

public record StudentLedgerResponse(
        Long studentId,
        String studentName,
        BigDecimal invoiced,
        BigDecimal discounts,
        BigDecimal paid,
        BigDecimal outstanding,
        List<InvoiceResponse> invoices,
        List<PaymentResponse> payments
) {
}
