package tz.co.chambaka.school.management.sms;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        if (digits.startsWith("0") && digits.length() == 10) {
            return "255" + digits.substring(1);
        }
        if (digits.startsWith("255") && digits.length() >= 12) {
            return digits;
        }
        return digits.isBlank() ? null : digits;
    }

    /**
     * Prefer {@code +255…} for the notification-service phone validator (soleiltech).
     */
    public static String toE164Like(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        String digits = trimmed.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.startsWith("255") && digits.length() >= 12) {
            return "+" + digits;
        }
        if (digits.startsWith("0") && digits.length() == 10) {
            return "+255" + digits.substring(1);
        }
        if (digits.length() == 9) {
            return "+255" + digits;
        }
        if (digits.isBlank()) {
            return null;
        }
        return digits.startsWith("+") ? digits : "+" + digits;
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 3);
    }
}
