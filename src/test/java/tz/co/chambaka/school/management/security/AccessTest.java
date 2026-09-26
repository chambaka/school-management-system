package tz.co.chambaka.school.management.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTest {

    @Test
    void expressionsCoverGuideModules() throws Exception {
        assertThat(Access.FEE_MANAGE).contains("ACCOUNTANT");
        assertThat(Access.ACADEMIC_LOOKUP).contains("ACCOUNTANT");
        assertThat(Access.TIMETABLE_MANAGE).contains("ACADEMIC_MASTER");
        assertThat(Access.EXAM_APPROVE).contains("HEADMASTER").contains("ACADEMIC_MASTER");
        assertThat(Access.TERM_RESULT).contains("STUDENT").contains("PARENT").contains("TEACHER");
        assertThat(Access.PEOPLE_MANAGE).contains("HEADMASTER").contains("SCHOOL_ADMIN");
        assertThat(Access.BUILDING_MANAGE).contains("HEADMASTER").contains("SCHOOL_ADMIN");
        assertThat(Access.BUILDING_MANAGE).doesNotContain("ACADEMIC_MASTER");
        assertThat(Access.EXAM_INVIGILATE).contains("INVIGILATOR");
        assertThat(Access.ACCOUNT_UNLOCK).contains("HEADMASTER").contains("ACADEMIC_MASTER");
        assertThat(Access.STAFF_OFFICERS).contains("SUPER_ADMIN").contains("HEADMASTER");
        assertThat(Access.ORG_RENAME).contains("SUPER_ADMIN").contains("ORGANIZATION_ADMIN");
        assertThat(Access.ORG_RENAME).doesNotContain("HEADMASTER").doesNotContain("SCHOOL_ADMIN");
        assertThat(Access.ORG_SCHOOLS).contains("ORGANIZATION_ADMIN").doesNotContain("HEADMASTER");
        assertThat(Access.OFFICER_MANAGE).contains("ORGANIZATION_ADMIN").contains("HEADMASTER");
        Constructor<Access> ctor = Access.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}
