package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AcademicYearRequest(
        @NotBlank String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        boolean currentYear
) {
}
