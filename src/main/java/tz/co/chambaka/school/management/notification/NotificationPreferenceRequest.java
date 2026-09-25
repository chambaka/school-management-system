package tz.co.chambaka.school.management.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationPreferenceRequest(
        @NotBlank String eventKey,
        boolean inApp,
        boolean email,
        boolean sms,
        boolean push
) {
}
