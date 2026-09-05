package tz.co.chambaka.school.management.config;

import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SmsPropertiesTest {

    @Test
    void defaultsPasswordResetWhenMissing() {
        SmsProperties fromLegacyConstructor = Fixtures.properties();
        assertThat(fromLegacyConstructor.passwordReset().ttl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(fromLegacyConstructor.passwordReset().includeDebugCode()).isFalse();

        SmsProperties fromNullReset = new SmsProperties(
                fromLegacyConstructor.jwt(),
                fromLegacyConstructor.cors(),
                fromLegacyConstructor.superAdmin(),
                null);
        assertThat(fromNullReset.passwordReset().ttl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(fromNullReset.passwordReset().includeDebugCode()).isFalse();
        assertThat(fromLegacyConstructor.singleTenant()).isFalse();
        assertThat(fromNullReset.tenancy().defaultTenantSlug()).isEqualTo("halo");
    }

    @Test
    void tenancyDefaultsBlankFieldsAndNullMode() {
        SmsProperties.Tenancy tenancy = new SmsProperties.Tenancy(null, "  ", "");
        assertThat(tenancy.mode()).isEqualTo(SmsProperties.Mode.MULTI);
        assertThat(tenancy.defaultTenantName()).isEqualTo("Halo Campus");
        assertThat(tenancy.defaultTenantSlug()).isEqualTo("halo");

        SmsProperties fromNullTenancy = new SmsProperties(
                Fixtures.properties().jwt(),
                Fixtures.properties().cors(),
                Fixtures.properties().superAdmin(),
                Fixtures.properties().passwordReset(),
                null);
        assertThat(fromNullTenancy.singleTenant()).isFalse();

        SmsProperties single = new SmsProperties(
                Fixtures.properties().jwt(),
                Fixtures.properties().cors(),
                Fixtures.properties().superAdmin(),
                Fixtures.properties().passwordReset(),
                new SmsProperties.Tenancy(SmsProperties.Mode.SINGLE, "Org", "org"));
        assertThat(single.singleTenant()).isTrue();
    }
}
