package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.model.enums.Role;

public final class AuditPathClassifier {

    private AuditPathClassifier() {
    }

    public static boolean isNoise(String path) {
        if (path == null) {
            return true;
        }
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/v1/public/")
                || path.startsWith("/api/v1/audit-events")
                || path.startsWith("/api/v1/platform/audit-events");
    }

    public static boolean isAuth(String path) {
        return path != null && path.startsWith("/api/v1/auth/");
    }

    public static boolean isFinance(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/v1/fees")
                || path.startsWith("/api/v1/invoices")
                || path.startsWith("/api/v1/payments");
    }

    public static AuditScope scope(String path, Role role, AuditAction action) {
        if (action == AuditAction.REGISTER_SCHOOL
                || action == AuditAction.REGISTER_TENANT
                || (path != null && path.startsWith("/api/v1/platform/"))
                || role == Role.SUPER_ADMIN) {
            return AuditScope.PLATFORM;
        }
        return AuditScope.TENANT;
    }

    public static AuditAction actionFromHttp(String method, int status) {
        if (status == 401 || status == 403) {
            return AuditAction.ACCESS_DENIED;
        }
        if (status >= 500) {
            return AuditAction.ERROR;
        }
        if (method == null) {
            return AuditAction.ACCESS;
        }
        return switch (method.toUpperCase()) {
            case "POST" -> AuditAction.CREATE;
            case "PUT", "PATCH" -> AuditAction.UPDATE;
            case "DELETE" -> AuditAction.DELETE;
            default -> AuditAction.ACCESS;
        };
    }

    public static String resourceType(String path) {
        if (path == null || path.isBlank()) {
            return "Unknown";
        }
        String[] parts = path.split("/");
        String candidate = null;
        for (String part : parts) {
            if (part.isBlank() || "api".equals(part) || "v1".equals(part) || "platform".equals(part)) {
                continue;
            }
            if (part.chars().allMatch(Character::isDigit)) {
                continue;
            }
            candidate = part;
            break;
        }
        if (candidate == null) {
            return "Unknown";
        }
        return toSingularType(candidate);
    }

    public static String resourceId(String path) {
        if (path == null) {
            return null;
        }
        String[] parts = path.split("/");
        for (String part : parts) {
            if (!part.isBlank() && part.chars().allMatch(Character::isDigit)) {
                return part;
            }
        }
        return null;
    }

    public static boolean shouldRecord(String method, String path, int status) {
        if (isNoise(path) || "OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        if (isAuth(path) && status < 500) {
            return false;
        }
        if (isFinance(path)) {
            return true;
        }
        boolean mutating = method != null && (
                "POST".equalsIgnoreCase(method)
                        || "PUT".equalsIgnoreCase(method)
                        || "PATCH".equalsIgnoreCase(method)
                        || "DELETE".equalsIgnoreCase(method));
        return mutating || status >= 400;
    }

    private static String toSingularType(String segment) {
        return switch (segment) {
            case "schools" -> "School";
            case "tenants" -> "Tenant";
            case "campuses" -> "Campus";
            case "students" -> "Student";
            case "teachers" -> "Teacher";
            case "school-admins" -> "SchoolAdmin";
            case "parents" -> "Parent";
            case "invoices" -> "Invoice";
            case "payments" -> "Payment";
            case "fees" -> "FeeStructure";
            case "notices" -> "Notice";
            case "exams" -> "Exam";
            case "grades" -> "Grade";
            case "attendance" -> "Attendance";
            case "academic-years" -> "AcademicYear";
            case "classes" -> "SchoolClass";
            case "sections" -> "Section";
            case "subjects" -> "Subject";
            case "departments" -> "Department";
            case "allocations" -> "TeacherSubject";
            case "timetable" -> "TimetableSlot";
            case "messages" -> "StudentMessage";
            case "communications" -> "StudentMessage";
            default -> Character.toUpperCase(segment.charAt(0)) + segment.substring(1);
        };
    }
}
