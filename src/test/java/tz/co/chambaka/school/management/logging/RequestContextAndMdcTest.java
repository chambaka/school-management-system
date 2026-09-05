package tz.co.chambaka.school.management.logging;

import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextAndMdcTest {

    @AfterEach
    void cleanup() {
        RequestContext.clear();
        RequestMdc.clear();
    }

    @Test
    void storesAndClearsRequestFields() {
        RequestContext.set("cid-12345", "10.0.0.8", "JUnit");
        assertThat(RequestContext.getCorrectionId()).isEqualTo("cid-12345");
        assertThat(RequestContext.requireCorrectionId()).isEqualTo("cid-12345");
        assertThat(RequestContext.getIpAddress()).isEqualTo("10.0.0.8");
        assertThat(RequestContext.getUserAgent()).isEqualTo("JUnit");
        RequestContext.clear();
        assertThat(RequestContext.getCorrectionId()).isNull();
        assertThat(RequestContext.requireCorrectionId()).isNotBlank();
    }

    @Test
    void mdcPutsActorAndBlankKeys() {
        RequestMdc.putCorrectionId("cid-1");
        RequestMdc.putActor(Fixtures.principal(Role.ADMIN));
        assertThat(MDC.get(RequestMdc.CORRECTION_ID)).isEqualTo("cid-1");
        assertThat(MDC.get(RequestMdc.USER_ID)).isEqualTo("10");
        assertThat(MDC.get(RequestMdc.SCHOOL_ID)).isEqualTo("1");
        assertThat(MDC.get(RequestMdc.ROLE)).isEqualTo("ADMIN");

        RequestMdc.putActor(null);
        RequestMdc.put("blank", "  ");
        assertThat(MDC.get("blank")).isNull();

        User user = Fixtures.user(3L, Role.SUPER_ADMIN);
        user.setSchoolId(null);
        RequestMdc.putActor(new UserPrincipal(user));
        assertThat(MDC.get(RequestMdc.SCHOOL_ID)).isNull();
        RequestMdc.clear();
        assertThat(MDC.get(RequestMdc.CORRECTION_ID)).isNull();
    }
}
