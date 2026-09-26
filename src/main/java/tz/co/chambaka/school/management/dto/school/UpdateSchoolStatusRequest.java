package tz.co.chambaka.school.management.dto.school;

import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSchoolStatusRequest(
        @NotNull SchoolStatus status
) {
}
