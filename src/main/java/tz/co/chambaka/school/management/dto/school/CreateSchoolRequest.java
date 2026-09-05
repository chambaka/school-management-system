package tz.co.chambaka.school.management.dto.school;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String campusName,
        String email,
        String phone,
        String timezone,
        String currency,
        String country
) {
}
