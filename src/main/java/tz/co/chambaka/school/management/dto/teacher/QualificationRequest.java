package tz.co.chambaka.school.management.dto.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QualificationRequest(
        @NotBlank @Size(max = 150) String name,
        Integer sortOrder
) {
}
