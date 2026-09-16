package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradingBandRequest(
        @NotNull Integer minPercent,
        @NotNull Integer maxPercent,
        @NotBlank String letter,
        @NotNull BigDecimal points,
        int sortOrder
) {
}
