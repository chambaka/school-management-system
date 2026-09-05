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
    }
}
