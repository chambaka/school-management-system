package tz.co.chambaka.school.management.security;

public final class Access {

    public static final String PEOPLE_MANAGE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER')";
    public static final String ACADEMIC_STAFF = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER')";
    public static final String ACADEMIC_VIEW = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','STAFF','INVIGILATOR','PARENT','STUDENT')";
    /** Years and classes needed to bill fees; not timetable or exam write. */
    public static final String ACADEMIC_LOOKUP = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','ACCOUNTANT')";
    public static final String STUDENT_DIRECTORY = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','ACCOUNTANT','STAFF')";
    public static final String TEACHER_MANAGE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER')";
    public static final String TIMETABLE_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String TIMETABLE_VIEW = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','STAFF','INVIGILATOR','PARENT','STUDENT')";
    public static final String ATTENDANCE_ENTER = "hasAnyRole('ACADEMIC_MASTER','TEACHER')";
    public static final String ATTENDANCE_MANAGE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER')";
    public static final String ATTENDANCE_VIEW = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','STAFF','PARENT','STUDENT')";
    public static final String EXAM_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String EXAM_VERIFY = "hasRole('ACADEMIC_MASTER')";
    public static final String EXAM_APPROVE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String EXAM_REJECT = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String EXAM_INVIGILATE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','INVIGILATOR')";
    public static final String GRADE_ENTER = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER')";
    public static final String ASSIGNMENT = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','STUDENT','PARENT')";
    public static final String ASSIGNMENT_WRITE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER')";
    public static final String ASSIGNMENT_SUBMIT = "hasRole('STUDENT')";
    public static final String LESSON_LOG = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER')";
    public static final String REPORT_EXPORT = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','ACCOUNTANT','TEACHER')";
    public static final String FEE_VIEW = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','ACCOUNTANT','PARENT')";
    public static final String FEE_MANAGE = "hasRole('ACCOUNTANT')";
    public static final String INVOICE_VIEW = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACCOUNTANT')";
    public static final String INVOICE_MANAGE = "hasRole('ACCOUNTANT')";
    public static final String REPORT_CARD = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','PARENT')";
    public static final String TERM_RESULT = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','PARENT','STUDENT')";
    public static final String PAYMENT_RECORD = "hasRole('ACCOUNTANT')";
    public static final String FINANCE_RECORD = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACCOUNTANT','PARENT','STUDENT')";
    public static final String NOTICE_WRITE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','ACCOUNTANT','STAFF')";
    public static final String MESSAGE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','ACCOUNTANT','PARENT')";
    public static final String AUDIT = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','ACCOUNTANT')";
    public static final String SCHOOL_USER = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER','ACCOUNTANT','STAFF','INVIGILATOR','STUDENT','PARENT')";
    public static final String ORG_SCHOOLS = "hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')";
    public static final String ORG_RENAME = "hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN')";
    public static final String OFFICER_MANAGE = "hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','HEADMASTER','SCHOOL_ADMIN')";
    public static final String STAFF_OFFICERS = "hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER')";
    public static final String ACCOUNT_UNLOCK = "hasAnyRole('SUPER_ADMIN','ORGANIZATION_ADMIN','HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER')";
    public static final String CURRICULUM = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN','ACADEMIC_MASTER','TEACHER')";
    public static final String BUILDING_MANAGE = "hasAnyRole('HEADMASTER','SCHOOL_ADMIN')";
    public static final String PERMISSIONS = "isAuthenticated()";

    private Access() {
    }
}
