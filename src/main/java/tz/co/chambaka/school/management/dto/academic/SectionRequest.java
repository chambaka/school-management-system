package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SectionRequest(
        @NotNull Long schoolClassId,
        @NotBlank String name,
        Integer capacity,
        Long classTeacherId
) {
}
