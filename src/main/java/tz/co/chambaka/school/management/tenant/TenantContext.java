package tz.co.chambaka.school.management.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> SCHOOL_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setSchoolId(Long schoolId) {
        SCHOOL_ID.set(schoolId);
    }

    public static Long getSchoolId() {
        return SCHOOL_ID.get();
    }

    public static void clear() {
        SCHOOL_ID.remove();
    }
}
