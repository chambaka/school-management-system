package tz.co.chambaka.school.management.security;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.support.Fixtures;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(Fixtures.properties());
    }

    @Test
    void generatesAndParsesAccessToken() {
        User user = Fixtures.user(9L, Role.ADMIN);
        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("9");
        assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("schoolId", Long.class)).isEqualTo(1L);
    }

    @Test
    void refreshTokenLooksRandom() {
        assertThat(jwtService.generateRefreshTokenValue()).contains(".");
        assertThat(jwtService.generateRefreshTokenValue()).isNotEqualTo(jwtService.generateRefreshTokenValue());
    }

    @Test
    void ttlAndExpiry() {
        assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(3600);
        assertThat(jwtService.refreshExpiry()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }

    @Test
    void rejectsShortSecret() {
        SmsProperties shortSecret = new SmsProperties(
                new SmsProperties.Jwt("too-short", Duration.ofHours(1), Duration.ofDays(1)),
                new SmsProperties.Cors(List.of("*")),
                new SmsProperties.SuperAdmin("a@b.c", "x", "n"));
        assertThatThrownBy(() -> new JwtService(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
