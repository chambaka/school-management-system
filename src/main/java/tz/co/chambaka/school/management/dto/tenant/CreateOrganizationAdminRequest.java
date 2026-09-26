package tz.co.chambaka.school.management.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationAdminRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @Size(min = 10, max = 72) String password,
        String phone
) {
}
