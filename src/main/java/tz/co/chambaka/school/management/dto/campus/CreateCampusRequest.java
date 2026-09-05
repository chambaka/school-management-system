package tz.co.chambaka.school.management.dto.campus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCampusRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 30) String code,
        String address,
        String phone,
        String email,
        String timezone,
        Boolean primaryCampus
) {
}
