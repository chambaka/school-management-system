package tz.co.chambaka.school.management.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrectionIdsTest {

    @Test
    void generatesAndValidates() {
        String id = CorrectionIds.newId();
        assertThat(CorrectionIds.isValid(id)).isTrue();
        assertThat(CorrectionIds.isValid(null)).isFalse();
        assertThat(CorrectionIds.isValid("short")).isFalse();
        assertThat(CorrectionIds.isValid("bad id with spaces!!")).isFalse();
        assertThat(CorrectionIds.normalize("  abc  ")).isEqualTo("abc");
        assertThat(CorrectionIds.normalize(null)).isNull();
    }

    @Test
    void resolvePrefersValidHeaders() {
        assertThat(CorrectionIds.resolve("corr-1234", "alias-99")).isEqualTo("corr-1234");
        assertThat(CorrectionIds.resolve("bad", "alias-99")).isEqualTo("alias-99");
        assertThat(CorrectionIds.resolve("bad", "no")).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void clientIpUsesForwardedFor() {
        assertThat(CorrectionIds.clientIp("10.0.0.8, 10.1.1.1", "127.0.0.1")).isEqualTo("10.0.0.8");
        assertThat(CorrectionIds.clientIp(",", "127.0.0.1")).isEqualTo("127.0.0.1");
        assertThat(CorrectionIds.clientIp("  ", "127.0.0.1")).isEqualTo("127.0.0.1");
        assertThat(CorrectionIds.clientIp(null, "127.0.0.1")).isEqualTo("127.0.0.1");
    }
}
