package tz.co.chambaka.school.management.notification;

public record NotificationTemplateResponse(
        Long id,
        String eventKey,
        NotificationChannel channel,
        String subject,
        String body
) {
}
