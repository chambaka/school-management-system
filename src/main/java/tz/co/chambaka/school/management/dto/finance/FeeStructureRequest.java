package tz.co.chambaka.school.management.dto.finance;

import tz.co.chambaka.school.management.model.enums.FeeFrequency;
import tz.co.chambaka.school.management.model.enums.FeeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeStructureRequest(
        @NotNull Long academicYearId,
        Long schoolClassId,
        @NotBlank String name,
        @NotNull FeeType feeType,
        @NotNull FeeFrequency frequency,
        @NotNull @Positive BigDecimal amount,
        LocalDate dueDate
) {
}
