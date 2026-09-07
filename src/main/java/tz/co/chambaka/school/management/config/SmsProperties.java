package tz.co.chambaka.school.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

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
    @ConstructorBinding
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

    public static SmsProperties of(Jwt jwt, Cors cors, SuperAdmin superAdmin) {
        return new SmsProperties(jwt, cors, superAdmin, null, null, null);
    }

    public static SmsProperties of(Jwt jwt, Cors cors, SuperAdmin superAdmin, PasswordReset passwordReset) {
        return new SmsProperties(jwt, cors, superAdmin, passwordReset, null, null);
    }

    public static SmsProperties of(Jwt jwt, Cors cors, SuperAdmin superAdmin, PasswordReset passwordReset, Tenancy tenancy) {
        return new SmsProperties(jwt, cors, superAdmin, passwordReset, tenancy, null);
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
                defaultTenantName = "ShuleHub";
            }
            if (defaultTenantSlug == null || defaultTenantSlug.isBlank()) {
                defaultTenantSlug = "shulehub";
            }
        }

        public static Tenancy defaults() {
            return new Tenancy(Mode.MULTI, "ShuleHub", "shulehub");
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
                senderId = "SHULEHUB";
            }
            if (apiKey == null) {
                apiKey = "";
            }
            if (path == null || path.isBlank()) {
                path = "/f1/queueNotification";
            }
        }

        public static Messaging of(Boolean enabled, String provider, String senderId, String baseUrl, String apiKey) {
            return new Messaging(enabled, provider, senderId, baseUrl, apiKey, "/f1/queueNotification");
        }

        public boolean active() {
            return Boolean.TRUE.equals(enabled);
        }

        public boolean notificationConfigured() {
            return baseUrl != null && !baseUrl.isBlank();
        }

        public static Messaging defaults() {
            return new Messaging(false, "", "SHULEHUB", "http://localhost:7575", "", "/f1/queueNotification");
        }
    }
}
