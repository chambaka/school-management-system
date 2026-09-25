package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.ResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeResponse;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.logging.RequestContext;
import tz.co.chambaka.school.management.model.PasswordResetToken;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.repository.PasswordResetTokenRepository;
import tz.co.chambaka.school.management.repository.RefreshTokenRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.PasswordPolicy;
import tz.co.chambaka.school.management.sms.CredentialSmsService;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import tz.co.chambaka.school.management.sms.SmsSendResult;
import tz.co.chambaka.school.management.util.TokenHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String GENERIC_MESSAGE =
            "If that account exists, we sent a reset code by SMS.";
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SmsProperties properties;
    private final CredentialSmsService credentialSms;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            SmsProperties properties,
            CredentialSmsService credentialSms
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.properties = properties;
        this.credentialSms = credentialSms;
    }

    @Transactional
    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request) {
        String identifier = request == null ? null : request.identifier();
        String lookup = identifier == null ? "" : identifier.trim();
        String masked = maskDestination(lookup);
        int ttlSeconds = (int) ttl().toSeconds();
        logReset("forgot.request",
                "identifier=" + identifier
                        + " lookup=" + lookup
                        + " maskedPhone=" + masked
                        + " ttlSeconds=" + ttlSeconds
                        + " includeDebugCode=" + includeDebugCode());
        User user = findByIdentifier(lookup);
        if (user == null || !user.isEnabled()) {
            logReset("forgot.unknown-or-disabled",
                    "lookup=" + lookup
                            + " userFound=" + (user != null)
                            + (user == null ? "" : " " + describeUser(user)));
            return new ForgotPasswordResponse(GENERIC_MESSAGE, masked, ttlSeconds, null);
        }
        var previous = resetTokenRepository
                .findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, Instant.now());
        previous.forEach(token -> token.setConsumed(true));
        String code = String.format("%06d", random.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(TokenHash.sha256(code));
        token.setExpiresAt(Instant.now().plus(ttl()));
        token.setConsumed(false);
        resetTokenRepository.save(token);
        SmsSendResult sms = credentialSms.sendResetCode(user, code, ttlSeconds);
        logReset("forgot.code-issued",
                describeUser(user)
                        + " consumedPrevious=" + previous.size()
                        + " tokenId=" + token.getId()
                        + " code=" + code
                        + " codeHash=" + token.getCodeHash()
                        + " expiresAt=" + token.getExpiresAt()
                        + " ttlSeconds=" + ttlSeconds
                        + " debugCodeReturned=" + includeDebugCode()
                        + " smsSent=" + (sms != null && sms.sent())
                        + " smsRef=" + (sms == null ? null : sms.providerRef())
                        + " smsError=" + (sms == null ? null : sms.error()));
        auditService.recordAuth(AuditAction.PASSWORD_RESET_REQUESTED, user, "Password reset code issued");
        return new ForgotPasswordResponse(
                GENERIC_MESSAGE,
                maskPhone(user.getPhone(), masked),
                ttlSeconds,
                includeDebugCode() ? code : null);
    }

    @Transactional
    public VerifyResetCodeResponse verifyCode(VerifyResetCodeRequest request) {
        String identifier = request == null || request.identifier() == null ? "" : request.identifier().trim();
        String code = request == null ? null : request.code();
        logReset("verify.request", "identifier=" + identifier + " code=" + code + " codeLen=" + len(code));
        User user = findByIdentifier(identifier);
        if (user == null) {
            logReset("verify.unknown-identifier", "identifier=" + identifier);
            throw invalidCode();
        }
        PasswordResetToken token = resetTokenRepository
                .findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, Instant.now())
                .stream()
                .findFirst()
                .orElse(null);
        if (token == null) {
            logReset("verify.no-active-token", describeUser(user) + " identifier=" + identifier);
            throw invalidCode();
        }
        logReset("verify.token",
                describeUser(user)
                        + " tokenId=" + token.getId()
                        + " failedAttempts=" + token.getFailedAttempts()
                        + " maxAttempts=" + MAX_ATTEMPTS
                        + " expiresAt=" + token.getExpiresAt()
                        + " consumed=" + token.isConsumed()
                        + " verifiedAt=" + token.getVerifiedAt()
                        + " codeHash=" + token.getCodeHash());
        if (token.getFailedAttempts() >= MAX_ATTEMPTS) {
            token.setConsumed(true);
            logReset("verify.locked", "tokenId=" + token.getId() + " failedAttempts=" + token.getFailedAttempts());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Too many invalid codes. Request a new reset.");
        }
        String presentedHash = TokenHash.sha256(code == null ? "" : code);
        if (!presentedHash.equals(token.getCodeHash())) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            boolean locked = token.getFailedAttempts() >= MAX_ATTEMPTS;
            if (locked) {
                token.setConsumed(true);
            }
            logReset("verify.code-mismatch",
                    "tokenId=" + token.getId()
                            + " presentedCode=" + code
                            + " presentedHash=" + presentedHash
                            + " expectedHash=" + token.getCodeHash()
                            + " failedAttempts=" + token.getFailedAttempts()
                            + " locked=" + locked);
            throw invalidCode();
        }
        String session = UUID.randomUUID().toString();
        token.setSessionHash(TokenHash.sha256(session));
        token.setVerifiedAt(Instant.now());
        int remaining = secondsRemaining(token);
        logReset("verify.ok",
                describeUser(user)
                        + " tokenId=" + token.getId()
                        + " session=" + session
                        + " sessionHash=" + token.getSessionHash()
                        + " verifiedAt=" + token.getVerifiedAt()
                        + " expiresInSeconds=" + remaining);
        return new VerifyResetCodeResponse(session, remaining);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String resetToken = request == null ? null : request.resetToken();
        String newPassword = request == null ? null : request.newPassword();
        logReset("reset.request",
                "resetToken=" + resetToken
                        + " resetTokenLen=" + len(resetToken)
                        + " resetTokenHash=" + TokenHash.sha256(resetToken == null ? "" : resetToken)
                        + " newPasswordLen=" + len(newPassword)
                        + " newPasswordBlank=" + (newPassword == null || newPassword.isBlank()));
        PasswordResetToken token = resetTokenRepository
                .findBySessionHashAndConsumedFalse(TokenHash.sha256(resetToken == null ? "" : resetToken))
                .orElse(null);
        if (token == null) {
            logReset("reset.invalid-session", "resetToken=" + resetToken);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset session");
        }
        logReset("reset.token",
                "tokenId=" + token.getId()
                        + " " + describeUser(token.getUser())
                        + " expiresAt=" + token.getExpiresAt()
                        + " verifiedAt=" + token.getVerifiedAt()
                        + " consumed=" + token.isConsumed()
                        + " failedAttempts=" + token.getFailedAttempts());
        if (token.getExpiresAt().isBefore(Instant.now()) || token.getVerifiedAt() == null) {
            token.setConsumed(true);
            logReset("reset.expired-or-unverified",
                    "tokenId=" + token.getId()
                            + " expired=" + token.getExpiresAt().isBefore(Instant.now())
                            + " verifiedAt=" + token.getVerifiedAt());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset session");
        }
        User user = token.getUser();
        try {
            PasswordPolicy.requireValid(newPassword, user.getEmail(), user.getName());
        } catch (RuntimeException ex) {
            logReset("reset.weak-password",
                    describeUser(user)
                            + " tokenId=" + token.getId()
                            + " newPasswordLen=" + len(newPassword)
                            + " reason=" + ex.getMessage());
            throw ex;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        token.setConsumed(true);
        refreshTokenRepository.deleteByUserId(user.getId());
        logReset("reset.ok", describeUser(user) + " tokenId=" + token.getId() + " refreshTokensRevoked=true");
        auditService.recordAuth(AuditAction.PASSWORD_RESET, user, "Password reset completed");
    }

    public static String maskEmail(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        char first = local.charAt(0);
        return first + "***" + domain;
    }

    public static String maskDestination(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "***";
        }
        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return maskEmail(trimmed);
        }
        String phone = PhoneNumbers.persist(trimmed);
        if (phone != null) {
            String e164 = PhoneNumbers.toE164Like(phone);
            return PhoneNumbers.mask(e164 == null ? phone : e164);
        }
        return "***";
    }

    private static String maskPhone(String phone, String fallback) {
        if (phone == null || phone.isBlank()) {
            return fallback;
        }
        String e164 = PhoneNumbers.toE164Like(phone);
        return PhoneNumbers.mask(e164 == null ? phone : e164);
    }

    private User findByIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim();
        if (key.contains("@")) {
            return userRepository.findByEmailIgnoreCase(key).orElse(null);
        }
        User byUsername = userRepository.findByUsernameIgnoreCase(key).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }
        String phone = PhoneNumbers.persist(key);
        if (phone != null) {
            User byPhone = userRepository.findFirstByPhone(phone).orElse(null);
            if (byPhone != null) {
                return byPhone;
            }
        }
        return userRepository.findByEmailIgnoreCase(key).orElse(null);
    }

    private Duration ttl() {
        SmsProperties.PasswordReset reset = properties.passwordReset();
        return reset == null || reset.ttl() == null ? Duration.ofMinutes(30) : reset.ttl();
    }

    private boolean includeDebugCode() {
        SmsProperties.PasswordReset reset = properties.passwordReset();
        return reset != null && Boolean.TRUE.equals(reset.includeDebugCode());
    }

    private int secondsRemaining(PasswordResetToken token) {
        long seconds = Duration.between(Instant.now(), token.getExpiresAt()).getSeconds();
        return (int) Math.max(0, seconds);
    }

    private ApiException invalidCode() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired code");
    }

    private void logReset(String step, String details) {
        log.info("password-reset step={} correctionId={} ip={} ua={} {}",
                step,
                RequestContext.getCorrectionId(),
                RequestContext.getIpAddress(),
                RequestContext.getUserAgent(),
                details);
    }

    private static String describeUser(User user) {
        if (user == null) {
            return "user=null";
        }
        return "userId=" + user.getId()
                + " email=" + user.getEmail()
                + " username=" + user.getUsername()
                + " phone=" + user.getPhone()
                + " role=" + user.getRole()
                + " enabled=" + user.isEnabled()
                + " tenantId=" + user.getTenantId()
                + " schoolId=" + user.getSchoolId()
                + " campusId=" + user.getCampusId();
    }

    private static int len(String value) {
        return value == null ? 0 : value.length();
    }
}
