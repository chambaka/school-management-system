package tz.co.chambaka.school.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "sms")
public record SmsProperties(
        Jwt jwt,
        Cors cors,
        SuperAdmin superAdmin,
        PasswordReset passwordReset,
        Tenancy tenancy
) {
    public SmsProperties {
        if (passwordReset == null) {
            passwordReset = new PasswordReset(Duration.ofMinutes(30), false);
        }
        if (tenancy == null) {
            tenancy = Tenancy.defaults();
        }
    }

    public SmsProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin) {
        this(jwt, cors, superAdmin, new PasswordReset(Duration.ofMinutes(30), false), Tenancy.defaults());
    }

    public SmsProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin, PasswordReset passwordReset) {
        this(jwt, cors, superAdmin, passwordReset, Tenancy.defaults());
    }

    public boolean singleTenant() {
        return tenancy != null && tenancy.mode() == Mode.SINGLE;
    }

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record SuperAdmin(String email, String password, String name) {
    }

    public record PasswordReset(Duration ttl, Boolean includeDebugCode) {
    }

    public enum Mode {
        MULTI,
        SINGLE
    }

    public record Tenancy(Mode mode, String defaultTenantName, String defaultTenantSlug) {
        public Tenancy {
            if (mode == null) {
                mode = Mode.MULTI;
            }
            if (defaultTenantName == null || defaultTenantName.isBlank()) {
                defaultTenantName = "Halo Campus";
            }
            if (defaultTenantSlug == null || defaultTenantSlug.isBlank()) {
                defaultTenantSlug = "halo";
            }
        }

        public static Tenancy defaults() {
            return new Tenancy(Mode.MULTI, "Halo Campus", "halo");
        }
    }
}
