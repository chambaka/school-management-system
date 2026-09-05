package tz.co.chambaka.school.management.dto.parent;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateParentRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        String phone,
        String occupation,
        String address
) {
}
