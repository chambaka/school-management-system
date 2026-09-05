package tz.co.chambaka.school.management.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 150) String name,
        String email,
        String phone,
        String country,
        String timezone,
        String currency
) {
}
