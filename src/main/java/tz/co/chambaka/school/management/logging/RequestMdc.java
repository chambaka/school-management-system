package tz.co.chambaka.school.management.logging;

import tz.co.chambaka.school.management.security.UserPrincipal;
import org.slf4j.MDC;

public final class RequestMdc {

    public static final String CORRECTION_ID = "correctionId";
    public static final String SCHOOL_ID = "schoolId";
    public static final String USER_ID = "userId";
    public static final String USER_EMAIL = "userEmail";
    public static final String ROLE = "role";

    private RequestMdc() {
    }

    public static void putCorrectionId(String correctionId) {
        put(CORRECTION_ID, correctionId);
    }

    public static void putActor(UserPrincipal principal) {
        if (principal == null) {
            return;
        }
        put(USER_ID, principal.getId() == null ? null : String.valueOf(principal.getId()));
        put(SCHOOL_ID, principal.getSchoolId() == null ? null : String.valueOf(principal.getSchoolId()));
        put(USER_EMAIL, principal.getEmail());
        put(ROLE, principal.getRole() == null ? null : principal.getRole().name());
    }

    public static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    public static void clear() {
        MDC.clear();
    }
}
