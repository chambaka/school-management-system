package tz.co.chambaka.school.management.dto.notification;

public record NotificationResponse(
        Long id,
        String title,
        String body,
        String category,
        boolean read,
        String createdAt
) {
}
