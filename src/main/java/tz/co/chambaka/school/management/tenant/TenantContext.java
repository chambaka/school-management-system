package tz.co.chambaka.school.management.tenant;

public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> SCHOOL_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CAMPUS_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void setSchoolId(Long schoolId) {
        SCHOOL_ID.set(schoolId);
    }

    public static Long getSchoolId() {
        return SCHOOL_ID.get();
    }

    public static void setCampusId(Long campusId) {
        CAMPUS_ID.set(campusId);
    }

    public static Long getCampusId() {
        return CAMPUS_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        SCHOOL_ID.remove();
        CAMPUS_ID.remove();
    }
}
