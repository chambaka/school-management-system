package tz.co.chambaka.school.management.model.enums;

import java.util.List;

public enum Role {
    SUPER_ADMIN,
    HEADMASTER,
    ACADEMIC_MASTER,
    TEACHER,
    ACCOUNTANT,
    PARENT,
    STUDENT;

    public boolean managesPeople() {
        return this == HEADMASTER || this == ACADEMIC_MASTER;
    }

    public boolean academicStaff() {
        return this == HEADMASTER || this == ACADEMIC_MASTER || this == TEACHER;
    }

    public boolean managesFinance() {
        return this == ACCOUNTANT;
    }

    public boolean viewsStudentFinance() {
        return this == ACCOUNTANT || this == HEADMASTER;
    }

    public boolean postsStudentMessages() {
        return managesPeople() || this == TEACHER || this == ACCOUNTANT || this == PARENT;
    }

    public boolean sendsParentSms() {
        return managesPeople();
    }

    public boolean switchesSchool() {
        return this == SUPER_ADMIN || this == HEADMASTER;
    }

    public static List<Role> schoolOfficers() {
        return List.of(HEADMASTER, ACADEMIC_MASTER, ACCOUNTANT);
    }
}
