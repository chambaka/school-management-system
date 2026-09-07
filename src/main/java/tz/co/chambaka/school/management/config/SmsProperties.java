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
        Tenancy tenancy,
        Messaging messaging
) {
    public SmsProperties {
        if (passwordReset == null) {
            passwordReset = new PasswordReset(Duration.ofMinutes(30), false);
        }
        if (tenancy == null) {
            tenancy = Tenancy.defaults();
        }
        if (messaging == null) {
            messaging = Messaging.defaults();
        }
    }

    public SmsProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin) {
        this(jwt, cors, superAdmin, new PasswordReset(Duration.ofMinutes(30), false), Tenancy.defaults(), Messaging.defaults());
    }

    public SmsProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin, PasswordReset passwordReset) {
        this(jwt, cors, superAdmin, passwordReset, Tenancy.defaults(), Messaging.defaults());
    }

    public SmsProperties(Jwt jwt, Cors cors, SuperAdmin superAdmin, PasswordReset passwordReset, Tenancy tenancy) {
        this(jwt, cors, superAdmin, passwordReset, tenancy, Messaging.defaults());
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

    /**
     * SMS via chambaka-notification-service ({@code POST /f1/queueNotification}), same as soleiltech.
     *
     * @param provider optional SMS backend override for the notification service
     *                 ({@code beem}, {@code textify}, {@code log-sms}); empty uses the service default
     */
    public record Messaging(
            Boolean enabled,
            String provider,
            String senderId,
            String baseUrl,
            String apiKey,
            String path
    ) {
        public Messaging {
            if (enabled == null) {
                enabled = false;
            }
            if (provider == null || provider.isBlank()) {
                provider = "";
            } else {
                provider = provider.trim();
            }
            if (senderId == null || senderId.isBlank()) {
                senderId = "HALO";
            }
            if (apiKey == null) {
                apiKey = "";
            }
            if (path == null || path.isBlank()) {
                path = "/f1/queueNotification";
            }
        }

        public Messaging(Boolean enabled, String provider, String senderId, String baseUrl, String apiKey) {
            this(enabled, provider, senderId, baseUrl, apiKey, "/f1/queueNotification");
        }

        public boolean active() {
            return Boolean.TRUE.equals(enabled);
        }

        public boolean notificationConfigured() {
            return baseUrl != null && !baseUrl.isBlank();
        }

        public static Messaging defaults() {
            return new Messaging(false, "", "HALO", "http://localhost:7575", "", "/f1/queueNotification");
        }
    }
}
