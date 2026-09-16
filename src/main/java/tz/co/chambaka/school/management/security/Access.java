package tz.co.chambaka.school.management.security;

public final class Access {

    public static final String PEOPLE_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String ACADEMIC_STAFF = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER')";
    public static final String ACADEMIC_VIEW = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','PARENT','STUDENT')";
    public static final String STUDENT_DIRECTORY = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','ACCOUNTANT')";
    public static final String TEACHER_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String TIMETABLE_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String TIMETABLE_VIEW = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','PARENT','STUDENT')";
    public static final String ATTENDANCE_ENTER = "hasAnyRole('ACADEMIC_MASTER','TEACHER')";
    public static final String ATTENDANCE_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String ATTENDANCE_VIEW = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','PARENT','STUDENT')";
    public static final String EXAM_MANAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER')";
    public static final String EXAM_VERIFY = "hasRole('ACADEMIC_MASTER')";
    public static final String EXAM_APPROVE = "hasRole('HEADMASTER')";
    public static final String GRADE_ENTER = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER')";
    public static final String ASSIGNMENT = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','STUDENT','PARENT')";
    public static final String ASSIGNMENT_WRITE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER')";
    public static final String LESSON_LOG = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER')";
    public static final String REPORT_EXPORT = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','ACCOUNTANT','TEACHER')";
    public static final String FEE_VIEW = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','ACCOUNTANT','PARENT')";
    public static final String FEE_MANAGE = "hasRole('ACCOUNTANT')";
    public static final String INVOICE_VIEW = "hasAnyRole('HEADMASTER','ACCOUNTANT')";
    public static final String INVOICE_MANAGE = "hasRole('ACCOUNTANT')";
    public static final String REPORT_CARD = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','PARENT')";
    public static final String PAYMENT_RECORD = "hasRole('ACCOUNTANT')";
    public static final String FINANCE_RECORD = "hasAnyRole('HEADMASTER','ACCOUNTANT','PARENT','STUDENT')";
    public static final String NOTICE_WRITE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','ACCOUNTANT')";
    public static final String MESSAGE = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','ACCOUNTANT','PARENT')";
    public static final String AUDIT = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','ACCOUNTANT')";
    public static final String SCHOOL_USER = "hasAnyRole('HEADMASTER','ACADEMIC_MASTER','TEACHER','ACCOUNTANT','STUDENT','PARENT')";
    public static final String ORG_SCHOOLS = "hasAnyRole('SUPER_ADMIN','HEADMASTER')";
    public static final String STAFF_OFFICERS = "hasAnyRole('SUPER_ADMIN','HEADMASTER','ACADEMIC_MASTER')";

    private Access() {
    }
}
