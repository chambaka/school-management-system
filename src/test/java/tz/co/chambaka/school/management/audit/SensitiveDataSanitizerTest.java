package tz.co.chambaka.school.management.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    @Test
    void masksSecretsAndTruncates() {
        assertThat(SensitiveDataSanitizer.sanitize(null)).isNull();
        assertThat(SensitiveDataSanitizer.sanitize("  ")).isEqualTo("  ");
        assertThat(SensitiveDataSanitizer.sanitize("{\"password\":\"secret\",\"name\":\"Ada\"}"))
                .contains("***")
                .doesNotContain("secret");
        assertThat(SensitiveDataSanitizer.sanitize("password=hunter2&x=1")).doesNotContain("hunter2");
        assertThat(SensitiveDataSanitizer.sanitize("{\"resetToken\":\"sess\",\"debugCode\":\"123456\"}"))
                .doesNotContain("sess")
                .doesNotContain("123456");
        assertThat(SensitiveDataSanitizer.isSensitiveKey("refresh_token")).isTrue();
        assertThat(SensitiveDataSanitizer.isSensitiveKey("resetToken")).isTrue();
        assertThat(SensitiveDataSanitizer.isSensitiveKey("email")).isFalse();
        assertThat(SensitiveDataSanitizer.isSensitiveKey(null)).isFalse();
        assertThat(SensitiveDataSanitizer.truncate("abc", 10)).isEqualTo("abc");
        assertThat(SensitiveDataSanitizer.truncate("abcdef", 3)).isEqualTo("abc...");
        assertThat(SensitiveDataSanitizer.truncate(null, 3)).isNull();
        String longBody = "x".repeat(4010);
        assertThat(SensitiveDataSanitizer.sanitize(longBody)).endsWith("...");
    }
}
