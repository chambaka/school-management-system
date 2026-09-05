package tz.co.chambaka.school.management.dto.auth;

import jakarta.validation.constraints.NotNull;

public record SwitchSchoolRequest(
        @NotNull Long schoolId,
        Long campusId
) {
}
