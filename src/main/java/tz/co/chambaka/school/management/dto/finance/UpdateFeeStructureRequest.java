package tz.co.chambaka.school.management.dto.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFeeStructureRequest(
        @NotNull @Positive BigDecimal amount,
        LocalDate dueDate
) {
}
