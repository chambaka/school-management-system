package tz.co.chambaka.school.management.dto.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InvoiceDiscountRequest(
        @NotNull @Positive BigDecimal amount,
        String reason
) {
}
