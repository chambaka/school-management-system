package tz.co.chambaka.school.management.audit;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "currentpassword", "newpassword", "oldpassword",
            "refreshtoken", "accesstoken", "resettoken", "token", "secret",
            "authorization", "debugcode"
    );

    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\"(?:password|currentPassword|newPassword|oldPassword|refreshToken|accessToken|resetToken|token|secret|debugCode)\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern FORM_SECRET = Pattern.compile(
            "(?i)((?:password|currentPassword|newPassword|refreshToken|resetToken|token)=)([^&]*)");

    private SensitiveDataSanitizer() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String masked = JSON_SECRET.matcher(raw).replaceAll("$1***$3");
        masked = FORM_SECRET.matcher(masked).replaceAll("$1***");
        return truncate(masked, 4000);
    }

    public static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEYS.contains(key.replace("_", "").toLowerCase(Locale.ROOT));
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
