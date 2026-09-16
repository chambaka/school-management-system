package tz.co.chambaka.school.management.dto.academic;

import tz.co.chambaka.school.management.model.enums.PeriodKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BellPeriodRequest(
        @NotBlank String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull PeriodKind kind,
        int sortOrder
) {
}
