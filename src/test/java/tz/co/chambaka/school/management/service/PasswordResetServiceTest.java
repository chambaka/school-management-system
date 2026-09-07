package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.ResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeResponse;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.PasswordResetToken;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.PasswordResetTokenRepository;
import tz.co.chambaka.school.management.repository.RefreshTokenRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import tz.co.chambaka.school.management.util.TokenHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    @Test
    void requestResetHidesUnknownAndDisabledAccounts() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        ForgotPasswordResponse missing = service(true).requestReset(new ForgotPasswordRequest("missing@example.com"));
        assertThat(missing.maskedEmail()).isEqualTo("m***@example.com");
        assertThat(missing.debugCode()).isNull();
        assertThat(missing.expiresInSeconds()).isEqualTo(1800);
        verify(resetTokenRepository, never()).save(any());

        User disabled = Fixtures.user(2L, Role.ADMIN);
        disabled.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(disabled));
        ForgotPasswordResponse hidden = service(false).requestReset(new ForgotPasswordRequest("admin@example.com"));
        assertThat(hidden.debugCode()).isNull();
        assertThat(hidden.message()).contains("If that account exists");
    }

    @Test
    void requestResetIssuesCodeAndConsumesPreviousTokens() {
        User user = Fixtures.user(2L, Role.ADMIN);
        PasswordResetToken previous = new PasswordResetToken();
        previous.setConsumed(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(previous));
        when(resetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordResponse response = service(Duration.ofMinutes(15), true)
                .requestReset(new ForgotPasswordRequest("admin@example.com"));

        assertThat(previous.isConsumed()).isTrue();
        assertThat(response.debugCode()).matches("\\d{6}");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getCodeHash()).isEqualTo(TokenHash.sha256(response.debugCode()));
        verify(auditService).recordAuth(AuditAction.PASSWORD_RESET_REQUESTED, user, "Password reset code issued");
    }

    @Test
    void requestResetOmitsDebugCodeWhenDisabledAndDefaultsTtl() {
        User user = Fixtures.user(2L, Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(resetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordResponse hidden = service(null, false)
                .requestReset(new ForgotPasswordRequest("admin@example.com"));
        assertThat(hidden.debugCode()).isNull();
        assertThat(hidden.expiresInSeconds()).isEqualTo(1800);

        ForgotPasswordResponse stillHidden = service(Duration.ofMinutes(30), null)
                .requestReset(new ForgotPasswordRequest("admin@example.com"));
        assertThat(stillHidden.debugCode()).isNull();

        SmsProperties noResetConfig = org.mockito.Mockito.mock(SmsProperties.class);
        when(noResetConfig.passwordReset()).thenReturn(null);
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());
        ForgotPasswordResponse fallback = new PasswordResetService(
                userRepository, resetTokenRepository, refreshTokenRepository,
                passwordEncoder, auditService, noResetConfig)
                .requestReset(new ForgotPasswordRequest("ghost@example.com"));
        assertThat(fallback.expiresInSeconds()).isEqualTo(1800);
        assertThat(fallback.debugCode()).isNull();
    }

    @Test
    void verifyCodeIssuesResetSession() {
        User user = Fixtures.user(2L, Role.ADMIN);
        PasswordResetToken token = pendingToken(user, "123456");
        token.setExpiresAt(Instant.now().minusSeconds(2));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(token));

        VerifyResetCodeResponse response = service(true)
                .verifyCode(new VerifyResetCodeRequest("admin@example.com", "123456"));
        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isZero();
        assertThat(token.getVerifiedAt()).isNotNull();
        assertThat(token.getSessionHash()).isEqualTo(TokenHash.sha256(response.resetToken()));
    }

    @Test
    void verifyCodeRejectsUnknownUserMissingTokenAndWrongCodes() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service(true).verifyCode(new VerifyResetCodeRequest("missing@example.com", "123456")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid or expired code");

        User user = Fixtures.user(2L, Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service(true).verifyCode(new VerifyResetCodeRequest("admin@example.com", "123456")))
                .isInstanceOf(ApiException.class);

        PasswordResetToken token = pendingToken(user, "123456");
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(token));
        assertThatThrownBy(() -> service(true).verifyCode(new VerifyResetCodeRequest("admin@example.com", "000000")))
                .isInstanceOf(ApiException.class);
        assertThat(token.getFailedAttempts()).isEqualTo(1);
        assertThat(token.isConsumed()).isFalse();
    }

    @Test
    void verifyCodeLocksAfterTooManyAttempts() {
        User user = Fixtures.user(2L, Role.ADMIN);
        PasswordResetToken alreadyLocked = pendingToken(user, "123456");
        alreadyLocked.setFailedAttempts(5);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(alreadyLocked));
        assertThatThrownBy(() -> service(true).verifyCode(new VerifyResetCodeRequest("admin@example.com", "123456")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Too many invalid codes");
        assertThat(alreadyLocked.isConsumed()).isTrue();

        PasswordResetToken lastTry = pendingToken(user, "123456");
        lastTry.setFailedAttempts(4);
        when(resetTokenRepository.findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(lastTry));
        assertThatThrownBy(() -> service(true).verifyCode(new VerifyResetCodeRequest("admin@example.com", "000000")))
                .isInstanceOf(ApiException.class);
        assertThat(lastTry.getFailedAttempts()).isEqualTo(5);
        assertThat(lastTry.isConsumed()).isTrue();
    }

    @Test
    void resetPasswordCompletesSessionAndRevokesRefreshTokens() {
        User user = Fixtures.user(2L, Role.ADMIN);
        PasswordResetToken token = pendingToken(user, "123456");
        token.setVerifiedAt(Instant.now());
        token.setSessionHash(TokenHash.sha256("session-1"));
        when(resetTokenRepository.findBySessionHashAndConsumedFalse(TokenHash.sha256("session-1")))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("HaloCampus1!")).thenReturn("hashed-new");

        service(true).resetPassword(new ResetPasswordRequest("session-1", "HaloCampus1!"));

        assertThat(user.getPassword()).isEqualTo("hashed-new");
        assertThat(token.isConsumed()).isTrue();
        verify(refreshTokenRepository).deleteByUserId(2L);
        verify(auditService).recordAuth(AuditAction.PASSWORD_RESET, user, "Password reset completed");
    }

    @Test
    void resetPasswordRejectsInvalidExpiredUnverifiedAndWeakPasswords() {
        when(resetTokenRepository.findBySessionHashAndConsumedFalse(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service(true).resetPassword(new ResetPasswordRequest("missing", "HaloCampus1!")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid or expired reset session");

        User user = Fixtures.user(2L, Role.ADMIN);
        PasswordResetToken expired = pendingToken(user, "123456");
        expired.setVerifiedAt(Instant.now());
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(resetTokenRepository.findBySessionHashAndConsumedFalse(any())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service(true).resetPassword(new ResetPasswordRequest("session-1", "HaloCampus1!")))
                .isInstanceOf(ApiException.class);
        assertThat(expired.isConsumed()).isTrue();

        PasswordResetToken unverified = pendingToken(user, "123456");
        when(resetTokenRepository.findBySessionHashAndConsumedFalse(any())).thenReturn(Optional.of(unverified));
        assertThatThrownBy(() -> service(true).resetPassword(new ResetPasswordRequest("session-1", "HaloCampus1!")))
                .isInstanceOf(ApiException.class);
        assertThat(unverified.isConsumed()).isTrue();

        PasswordResetToken verified = pendingToken(user, "123456");
        verified.setVerifiedAt(Instant.now());
        when(resetTokenRepository.findBySessionHashAndConsumedFalse(any())).thenReturn(Optional.of(verified));
        assertThatThrownBy(() -> service(true).resetPassword(new ResetPasswordRequest("session-1", "secret12")))
                .isInstanceOf(BusinessException.class);
        assertThat(verified.isConsumed()).isFalse();
    }

    @Test
    void maskEmailCoversShortAndInvalidValues() {
        assertThat(PasswordResetService.maskEmail("ab@x.com")).isEqualTo("a***@x.com");
        assertThat(PasswordResetService.maskEmail("@x.com")).isEqualTo("***");
        assertThat(PasswordResetService.maskEmail("no-at")).isEqualTo("***");
        assertThat(PasswordResetService.maskEmail("x@")).isEqualTo("x***@");
    }

    private PasswordResetService service(boolean debug) {
        return service(Duration.ofMinutes(30), debug);
    }

    private PasswordResetService service(Duration ttl, Boolean debug) {
        SmsProperties properties = SmsProperties.of(
                Fixtures.properties().jwt(),
                Fixtures.properties().cors(),
                Fixtures.properties().superAdmin(),
                new SmsProperties.PasswordReset(ttl, debug));
        return new PasswordResetService(
                userRepository,
                resetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                auditService,
                properties);
    }

    private static PasswordResetToken pendingToken(User user, String code) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(TokenHash.sha256(code));
        token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        token.setConsumed(false);
        token.setFailedAttempts(0);
        return token;
    }
}
