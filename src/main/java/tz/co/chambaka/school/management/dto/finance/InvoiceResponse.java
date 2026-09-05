package tz.co.chambaka.school.management.dto.finance;

import tz.co.chambaka.school.management.model.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        Long studentId,
        String studentName,
        String admissionNo,
        String invoiceNumber,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal balance,
        InvoiceStatus status,
        LocalDate dueDate,
        Instant issuedAt,
        List<InvoiceItemResponse> items
) {
}
