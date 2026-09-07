package tz.co.chambaka.school.management.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void schoolGuideAccess() {
        assertThat(Role.HEADMASTER.managesPeople()).isTrue();
        assertThat(Role.ACADEMIC_MASTER.academicStaff()).isTrue();
        assertThat(Role.TEACHER.academicStaff()).isTrue();
        assertThat(Role.ACCOUNTANT.managesFinance()).isTrue();
        assertThat(Role.HEADMASTER.viewsStudentFinance()).isTrue();
        assertThat(Role.ACADEMIC_MASTER.viewsStudentFinance()).isFalse();
        assertThat(Role.TEACHER.postsStudentMessages()).isTrue();
        assertThat(Role.ACCOUNTANT.sendsParentSms()).isFalse();
        assertThat(Role.HEADMASTER.switchesSchool()).isTrue();
        assertThat(Role.schoolOfficers()).containsExactly(Role.HEADMASTER, Role.ACADEMIC_MASTER, Role.ACCOUNTANT);
    }
}
