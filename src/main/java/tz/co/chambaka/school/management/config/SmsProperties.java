package tz.co.chambaka.school.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "sms")
public record SmsProperties(
        Jwt jwt,
        Cors cors,
        SuperAdmin superAdmin,
        PasswordReset passwordReset
) {
    public SmsProperties {
        if (passwordReset == null) {
            passwordReset = new PasswordReset(Duration.ofMinutes(30), false);
        }
    }

    public SmsProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin) {
        this(jwt, cors, superAdmin, new PasswordReset(Duration.ofMinutes(30), false));
    }

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record SuperAdmin(String email, String password, String name) {
    }

    public record PasswordReset(Duration ttl, Boolean includeDebugCode) {
    }
}
