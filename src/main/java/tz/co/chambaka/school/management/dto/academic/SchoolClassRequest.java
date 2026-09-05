package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SchoolClassRequest(
        @NotNull Long academicYearId,
        @NotBlank String name,
        @NotBlank String code,
        int displayOrder
) {
}
