package tz.co.chambaka.school.management.model.enums;

import java.util.List;

public enum Role {
    SUPER_ADMIN,
    ORGANIZATION_ADMIN,
    SCHOOL_ADMIN,
    HEADMASTER,
    ACADEMIC_MASTER,
    TEACHER,
    ACCOUNTANT,
    STAFF,
    INVIGILATOR,
    PARENT,
    STUDENT;

    public boolean managesPeople() {
        return this == HEADMASTER || this == ACADEMIC_MASTER || this == SCHOOL_ADMIN;
    }

    public boolean academicStaff() {
        return this == HEADMASTER || this == ACADEMIC_MASTER || this == TEACHER;
    }

    public boolean managesFinance() {
        return this == ACCOUNTANT;
    }

    public boolean viewsStudentFinance() {
        return this == ACCOUNTANT || this == HEADMASTER || this == SCHOOL_ADMIN;
    }

    public boolean postsStudentMessages() {
        return managesPeople() || this == TEACHER || this == ACCOUNTANT || this == PARENT;
    }

    public boolean sendsParentSms() {
        return managesPeople();
    }

    public boolean switchesSchool() {
        return this == SUPER_ADMIN;
    }

    public boolean schoolOperations() {
        return this == HEADMASTER || this == SCHOOL_ADMIN;
    }

    public boolean invigilates() {
        return this == INVIGILATOR || academicStaff();
    }

    public static List<Role> schoolOfficers() {
        return List.of(HEADMASTER, SCHOOL_ADMIN, ACADEMIC_MASTER, ACCOUNTANT, STAFF, INVIGILATOR);
    }
}
