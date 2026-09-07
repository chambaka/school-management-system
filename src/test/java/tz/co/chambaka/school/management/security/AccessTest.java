package tz.co.chambaka.school.management.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTest {

    @Test
    void expressionsCoverGuideModules() throws Exception {
        assertThat(Access.FEE_MANAGE).contains("ACCOUNTANT");
        assertThat(Access.TIMETABLE_MANAGE).contains("ACADEMIC_MASTER");
        assertThat(Access.PEOPLE_MANAGE).contains("HEADMASTER");
        Constructor<Access> ctor = Access.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}
