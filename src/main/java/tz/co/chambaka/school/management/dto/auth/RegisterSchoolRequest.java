package tz.co.chambaka.school.management.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterSchoolRequest(
        @Size(max = 150) String schoolName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 150) String adminName,
        String phone,
        String timezone,
        String currency,
        String country,
        @NotBlank @Size(max = 150) String tenantName,
        @Size(max = 150) String campusName
) {
}
