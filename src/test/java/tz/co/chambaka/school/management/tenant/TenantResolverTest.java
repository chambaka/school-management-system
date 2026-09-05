package tz.co.chambaka.school.management.tenant;

import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantResolverTest {

    private final TenantResolver resolver = new TenantResolver();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void requireSchoolId() {
        TenantContext.setSchoolId(7L);
        assertThat(resolver.requireSchoolId()).isEqualTo(7L);
    }

    @Test
    void requireSchoolIdMissing() {
        assertThatThrownBy(resolver::requireSchoolId)
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void superAdminUsesRequestedSchool() {
        assertThat(resolver.resolve(99L, Fixtures.principal(Role.SUPER_ADMIN))).isEqualTo(99L);
    }

    @Test
    void superAdminFallsBackToToken() {
        TenantContext.setSchoolId(5L);
        assertThat(resolver.resolve(null, Fixtures.principal(Role.SUPER_ADMIN))).isEqualTo(5L);
    }

    @Test
    void superAdminNeedsSchoolId() {
        assertThatThrownBy(() -> resolver.resolve(null, Fixtures.principal(Role.SUPER_ADMIN)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void schoolUserUsesOwnTenant() {
        assertThat(resolver.resolve(999L, Fixtures.principal(Role.ADMIN))).isEqualTo(1L);
    }
}
