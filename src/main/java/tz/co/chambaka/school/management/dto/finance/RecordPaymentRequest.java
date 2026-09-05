package tz.co.chambaka.school.management.dto.finance;

import tz.co.chambaka.school.management.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RecordPaymentRequest(
        @NotNull Long invoiceId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method,
        String transactionRef
) {
}
