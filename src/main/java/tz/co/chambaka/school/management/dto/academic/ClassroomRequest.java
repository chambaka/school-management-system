package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassroomRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30) String code,
        @Min(1) Integer capacity,
        @Size(max = 100) String building,
        @Size(max = 500) String notes
) {
}
