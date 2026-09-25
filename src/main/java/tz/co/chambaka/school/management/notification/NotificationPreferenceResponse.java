package tz.co.chambaka.school.management.notification;

public record NotificationPreferenceResponse(
        String eventKey,
        boolean inApp,
        boolean email,
        boolean sms,
        boolean push
) {
}
