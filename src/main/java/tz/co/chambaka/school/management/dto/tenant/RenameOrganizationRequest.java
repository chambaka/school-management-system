package tz.co.chambaka.school.management.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameOrganizationRequest(
        @NotBlank @Size(max = 150) String name
) {
}
