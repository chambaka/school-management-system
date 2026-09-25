package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String notes
) {
}
