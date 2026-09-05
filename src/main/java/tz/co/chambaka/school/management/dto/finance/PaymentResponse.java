package tz.co.chambaka.school.management.dto.finance;

import tz.co.chambaka.school.management.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        Long studentId,
        String studentName,
        BigDecimal amount,
        PaymentMethod method,
        String transactionRef,
        String receiptNumber,
        Instant paidAt
) {
}
