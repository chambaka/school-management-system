package tz.co.chambaka.school.management.security;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SmsProperties properties;
    private final SecretKey key;

    public JwtService(SmsProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("sms.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.jwt().accessTokenTtl());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("tenantId", user.getTenantId())
                .claim("schoolId", user.getSchoolId())
                .claim("campusId", user.getCampusId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshTokenValue() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    public long accessTokenTtlSeconds() {
        return properties.jwt().accessTokenTtl().toSeconds();
    }

    public Instant refreshExpiry() {
        return Instant.now().plus(properties.jwt().refreshTokenTtl());
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
