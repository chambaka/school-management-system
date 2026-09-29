package tz.co.chambaka.school.management.permission;

import tz.co.chambaka.school.management.model.enums.Role;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PermissionCatalog {

    public static final String PEOPLE_MANAGE = "people.manage";
    public static final String EXAM_APPROVE = "exam.approve";
    public static final String EXAM_VERIFY = "exam.verify";
    public static final String EXAM_INVIGILATE = "exam.invigilate";
    public static final String FINANCE_MANAGE = "finance.manage";
    public static final String ATTENDANCE_ENTER = "attendance.enter";
    public static final String REPORT_EXPORT = "report.export";
    public static final String TIMETABLE_MANAGE = "timetable.manage";
    public static final String SCHOOL_SWITCH = "school.switch";

    private PermissionCatalog() {
    }

    public static Map<String, String> definitions() {
        Map<String, String> all = new LinkedHashMap<>();
        all.put(PEOPLE_MANAGE, "Create and update students, teachers, and parents");
        all.put(EXAM_APPROVE, "Approve and publish results");
        all.put(EXAM_VERIFY, "Verify entered marks");
        all.put(EXAM_INVIGILATE, "View exam seats and papers");
        all.put(FINANCE_MANAGE, "Generate invoices and record payments");
        all.put(ATTENDANCE_ENTER, "Mark student attendance");
        all.put(REPORT_EXPORT, "Download reports");
        all.put(TIMETABLE_MANAGE, "Edit and generate the timetable");
        all.put(SCHOOL_SWITCH, "Switch the active school");
        return all;
    }

    public static List<String> forRole(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> List.copyOf(definitions().keySet());
            case ORGANIZATION_ADMIN -> List.of();
            case HEADMASTER -> List.of(PEOPLE_MANAGE, EXAM_APPROVE, EXAM_VERIFY, EXAM_INVIGILATE, REPORT_EXPORT);
            case SCHOOL_ADMIN -> List.of(PEOPLE_MANAGE, EXAM_INVIGILATE, REPORT_EXPORT);
            case ACADEMIC_MASTER -> List.of(PEOPLE_MANAGE, EXAM_APPROVE, EXAM_VERIFY, EXAM_INVIGILATE, ATTENDANCE_ENTER, TIMETABLE_MANAGE, REPORT_EXPORT);
            case TEACHER -> List.of(EXAM_INVIGILATE, ATTENDANCE_ENTER, REPORT_EXPORT);
            case ACCOUNTANT -> List.of(FINANCE_MANAGE, REPORT_EXPORT);
            case STAFF -> List.of();
            case INVIGILATOR -> List.of(EXAM_INVIGILATE);
            case PARENT, STUDENT -> List.of();
        };
    }

    public static Set<Role> roles() {
        return Set.of(Role.values());
    }
}
