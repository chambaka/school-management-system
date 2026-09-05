package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description
) {
}
