package tz.co.chambaka.school.management.logging;

import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrectionIds {

    public static final String HEADER = "X-Correction-Id";
    public static final String ALIAS_HEADER = "X-Correlation-Id";

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    private CorrectionIds() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String resolve(String preferred, String alias) {
        if (isValid(preferred)) {
            return preferred;
        }
        if (isValid(alias)) {
            return alias;
        }
        return newId();
    }

    public static boolean isValid(String value) {
        return value != null && VALID.matcher(value.trim()).matches();
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public static String clientIp(String forwardedFor, String remoteAddr) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",", 2);
            String first = hops.length == 0 ? "" : hops[0].trim();
            return first.isEmpty() ? remoteAddr : first;
        }
        return remoteAddr;
    }

}
