package tz.co.chambaka.school.management.dto.school;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSchoolRequest(
        @NotBlank @Size(max = 150) String name
) {
}
