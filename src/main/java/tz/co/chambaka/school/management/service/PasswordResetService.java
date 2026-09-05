package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.ResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeResponse;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.model.PasswordResetToken;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.repository.PasswordResetTokenRepository;
import tz.co.chambaka.school.management.repository.RefreshTokenRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.PasswordPolicy;
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
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String GENERIC_MESSAGE =
            "If that account exists, we sent a reset code. Check your email and spam folder.";
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SmsProperties properties;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            SmsProperties properties
    ) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String masked = maskEmail(email);
        int ttlSeconds = (int) ttl().toSeconds();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled()) {
            log.info("Password reset requested for unknown or disabled email masked={}", masked);
            return new ForgotPasswordResponse(GENERIC_MESSAGE, masked, ttlSeconds, null);
        }
        resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, Instant.now())
                .forEach(token -> token.setConsumed(true));
        String code = String.format("%06d", random.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(TokenHash.sha256(code));
        token.setExpiresAt(Instant.now().plus(ttl()));
        token.setConsumed(false);
        resetTokenRepository.save(token);
        log.info("Password reset code issued userId={} expiresAt={} debug={}",
                user.getId(), token.getExpiresAt(), includeDebugCode() ? code : "hidden");
        auditService.recordAuth(AuditAction.PASSWORD_RESET_REQUESTED, user, "Password reset code issued");
        return new ForgotPasswordResponse(
                GENERIC_MESSAGE,
                masked,
                ttlSeconds,
                includeDebugCode() ? code : null);
    }

    @Transactional
    public VerifyResetCodeResponse verifyCode(VerifyResetCodeRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null) {
            throw invalidCode();
        }
        PasswordResetToken token = resetTokenRepository
                .findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user, Instant.now())
                .stream()
                .findFirst()
                .orElseThrow(this::invalidCode);
        if (token.getFailedAttempts() >= MAX_ATTEMPTS) {
            token.setConsumed(true);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Too many invalid codes. Request a new reset.");
        }
        if (!TokenHash.sha256(request.code()).equals(token.getCodeHash())) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            if (token.getFailedAttempts() >= MAX_ATTEMPTS) {
                token.setConsumed(true);
            }
            throw invalidCode();
        }
        String session = UUID.randomUUID().toString();
        token.setSessionHash(TokenHash.sha256(session));
        token.setVerifiedAt(Instant.now());
        log.info("Password reset code verified userId={}", user.getId());
        return new VerifyResetCodeResponse(session, secondsRemaining(token));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository
                .findBySessionHashAndConsumedFalse(TokenHash.sha256(request.resetToken()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset session"));
        if (token.getExpiresAt().isBefore(Instant.now()) || token.getVerifiedAt() == null) {
            token.setConsumed(true);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired reset session");
        }
        User user = token.getUser();
        PasswordPolicy.requireValid(request.newPassword(), user.getEmail(), user.getName());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        token.setConsumed(true);
        refreshTokenRepository.deleteByUserId(user.getId());
        log.info("Password reset completed userId={}", user.getId());
        auditService.recordAuth(AuditAction.PASSWORD_RESET, user, "Password reset completed");
    }

    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        char first = local.charAt(0);
        return first + "***" + domain;
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
}
