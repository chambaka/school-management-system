package tz.co.chambaka.school.management.dto.communication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentMessageRequest(
        @NotBlank @Size(max = 2000) String body,
        boolean notifyParentsSms
) {
}
