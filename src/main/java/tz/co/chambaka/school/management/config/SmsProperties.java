package tz.co.chambaka.school.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "sms")
public record SmsProperties(
        Jwt jwt,
        Cors cors,
        SuperAdmin superAdmin
) {
    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record SuperAdmin(String email, String password, String name) {
    }
}
