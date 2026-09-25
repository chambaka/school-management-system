package tz.co.chambaka.school.management.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationTemplateRequest(
        @NotBlank String eventKey,
        NotificationChannel channel,
        @NotBlank String subject,
        @NotBlank String body
) {
}
