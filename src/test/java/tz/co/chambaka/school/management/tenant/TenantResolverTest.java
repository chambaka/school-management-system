package tz.co.chambaka.school.management.tenant;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResolverTest {

    @Mock
    private SmsProperties properties;
    @Mock
    private TenantRepository tenantRepository;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private TenantResolver resolver() {
        return new TenantResolver(properties, tenantRepository);
    }

    @Test
    void requireSchoolId() {
        TenantContext.setSchoolId(7L);
        assertThat(resolver().requireSchoolId()).isEqualTo(7L);
    }

    @Test
    void requireSchoolIdMissing() {
        assertThatThrownBy(() -> resolver().requireSchoolId())
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void superAdminUsesRequestedSchool() {
        assertThat(resolver().resolve(99L, Fixtures.principal(Role.SUPER_ADMIN))).isEqualTo(99L);
    }

    @Test
    void superAdminFallsBackToToken() {
        TenantContext.setSchoolId(5L);
        assertThat(resolver().resolve(null, Fixtures.principal(Role.SUPER_ADMIN))).isEqualTo(5L);
    }

    @Test
    void superAdminNeedsSchoolId() {
        assertThatThrownBy(() -> resolver().resolve(null, Fixtures.principal(Role.SUPER_ADMIN)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void schoolUserUsesOwnTenant() {
        assertThat(resolver().resolve(999L, Fixtures.principal(Role.ADMIN))).isEqualTo(1L);
    }

    @Test
    void requireTenantId() {
        TenantContext.setTenantId(10L);
        assertThat(resolver().requireTenantId()).isEqualTo(10L);
    }

    @Test
    void requireTenantIdMissing() {
        assertThatThrownBy(() -> resolver().requireTenantId())
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requireTenantIdFallsBackInSingleTenant() {
        when(properties.singleTenant()).thenReturn(true);
        when(properties.tenancy()).thenReturn(SmsProperties.Tenancy.defaults());
        when(tenantRepository.findBySlug("halo")).thenReturn(Optional.of(Fixtures.tenant()));
        assertThat(resolver().requireTenantId()).isEqualTo(10L);
    }

    @Test
    void requireTenantIdSingleMissingDefault() {
        when(properties.singleTenant()).thenReturn(true);
        when(properties.tenancy()).thenReturn(SmsProperties.Tenancy.defaults());
        when(tenantRepository.findBySlug("halo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver().requireTenantId())
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tenantAdminCanRequestSchool() {
        assertThat(resolver().resolve(99L, Fixtures.principal(Role.TENANT_ADMIN))).isEqualTo(99L);
    }

    @Test
    void tenantAdminFallsBackToHomeSchool() {
        assertThat(resolver().resolve(null, Fixtures.principal(Role.TENANT_ADMIN))).isEqualTo(1L);
    }
}
