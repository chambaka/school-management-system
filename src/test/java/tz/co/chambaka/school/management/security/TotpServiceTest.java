package tz.co.chambaka.school.management.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatesGoogleAuthenticatorCompatibleSecretAndCode() {
        String secret = totpService.newSecret();
        assertThat(secret).matches("[A-Z2-7]{32}");
        String uri = totpService.otpauthUri("teacher@school.ac.tz", secret);
        assertThat(uri).startsWith("otpauth://totp/ShuleHub%202FA:teacher%40school.ac.tz");
        assertThat(uri).contains("issuer=ShuleHub%202FA");
        assertThat(uri).contains("algorithm=SHA1");
        assertThat(totpService.verify(secret, TotpService.generate(TotpService.decodeBase32(secret),
                java.time.Instant.now().getEpochSecond() / 30))).isTrue();
        assertThat(totpService.verify(secret, "000000")).isFalse();
        assertThat(totpService.verify(secret, "12a456")).isFalse();
        assertThat(totpService.verify("not-a-secret", "123456")).isFalse();
    }

    @Test
    void acceptsSecretWithSpaces() {
        String secret = totpService.newSecret();
        String grouped = secret.replaceAll("(.{4})", "$1 ").trim();
        assertThat(totpService.verify(grouped, TotpService.generate(TotpService.decodeBase32(secret),
                java.time.Instant.now().getEpochSecond() / 30))).isTrue();
    }
}
