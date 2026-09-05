package tz.co.chambaka.school.management.logging;

public final class RequestContext {

    private static final ThreadLocal<String> CORRECTION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> IP_ADDRESS = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String correctionId, String ipAddress, String userAgent) {
        CORRECTION_ID.set(correctionId);
        IP_ADDRESS.set(ipAddress);
        USER_AGENT.set(userAgent);
    }

    public static String getCorrectionId() {
        return CORRECTION_ID.get();
    }

    public static String requireCorrectionId() {
        String id = CORRECTION_ID.get();
        return id == null || id.isBlank() ? CorrectionIds.newId() : id;
    }

    public static String getIpAddress() {
        return IP_ADDRESS.get();
    }

    public static String getUserAgent() {
        return USER_AGENT.get();
    }

    public static void clear() {
        CORRECTION_ID.remove();
        IP_ADDRESS.remove();
        USER_AGENT.remove();
    }
}
