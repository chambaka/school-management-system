package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.AdminResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.UserMapper;
import tz.co.chambaka.school.management.model.RefreshToken;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.model.enums.TenantStatus;
import tz.co.chambaka.school.management.repository.RefreshTokenRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.security.JwtService;
import tz.co.chambaka.school.management.sms.CredentialSmsService;
import tz.co.chambaka.school.management.sms.SmsSendResult;
import tz.co.chambaka.school.management.support.Fixtures;
import tz.co.chambaka.school.management.util.TokenHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantService tenantService;
    @Mock
    private CampusService campusService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuditService auditService;
    @Mock
    private SmsProperties smsProperties;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private CredentialSmsService credentialSms;
    @Mock
    private tz.co.chambaka.school.management.ratelimit.LoginRateLimitService loginRateLimitService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSuccess() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        School school = Fixtures.school();
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        stubActiveTenant();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        stubTokens(user);
        var response = authService.login(new LoginRequest("headmaster@example.com", "pw"));
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(authenticationManager).authenticate(any());
        verify(auditService).recordAuth(AuditAction.LOGIN, user, "User logged in");
    }

    @Test
    void loginReturnsTwoFactorChallengeWhenEnabled() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        stubActiveTenant();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(twoFactorService.isRequired(any())).thenReturn(true);
        when(twoFactorService.startChallenge(user)).thenReturn(
                tz.co.chambaka.school.management.dto.auth.LoginResponse.challenge("pending", true, "SECRET", "otpauth://totp/x"));
        var response = authService.login(new LoginRequest("headmaster@example.com", "pw"));
        assertThat(response.twoFactorRequired()).isTrue();
        assertThat(response.pendingToken()).isEqualTo("pending");
        assertThat(response.accessToken()).isNull();
        verify(jwtService, never()).generateAccessToken(any());
        verify(auditService, never()).recordAuth(eq(AuditAction.LOGIN), any(), any());
    }

    @Test
    void verifyTwoFactorIssuesTokens() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(twoFactorService.verify(any())).thenReturn(user);
        stubTokens(user);
        var response = authService.verifyTwoFactor(
                new tz.co.chambaka.school.management.dto.auth.VerifyTwoFactorRequest("pending", "123456"));
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.twoFactorRequired()).isFalse();
        verify(auditService).recordAuth(AuditAction.LOGIN, user, "User logged in");
    }

    @Test
    void loginFailedIsAudited() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthService.INVALID_LOGIN);
        verify(auditService).recordAuthFailure("headmaster@example.com", AuthService.INVALID_LOGIN);
    }

    @Test
    void loginAcceptsUsernameAndPhoneIdentifiers() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setUsername("head");
        when(userRepository.findByUsernameIgnoreCase("head")).thenReturn(Optional.of(user));
        stubActiveTenant();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        stubTokens(user);
        assertThat(authService.login(new LoginRequest(null, " head ", "pw")).accessToken()).isEqualTo("access");

        when(userRepository.findByUsernameIgnoreCase("0753493500")).thenReturn(Optional.empty());
        when(userRepository.findFirstByPhone("255753493500")).thenReturn(Optional.of(user));
        assertThat(authService.login(new LoginRequest(null, "0753493500", "pw")).accessToken()).isEqualTo("access");
    }

    @Test
    void fifthFailedLoginLocksAccountForFifteenMinutes() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        for (int attempt = 1; attempt < AuthService.MAX_FAILED_LOGINS; attempt++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                    .isInstanceOf(BadCredentialsException.class);
        }
        Instant before = Instant.now();
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("locked");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isBetween(before.plusSeconds(14 * 60), before.plusSeconds(16 * 60));
    }

    @Test
    void loginRejectsBlankAndAlreadyLockedIdentifier() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(null, " ", "pw")))
                .isInstanceOf(BusinessException.class);

        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setLockedUntil(Instant.now().plusSeconds(60));
        when(userRepository.findByUsernameIgnoreCase("head")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login(new LoginRequest(null, "head", "pw")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginDisabled() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginSuspendedSchool() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        School school = Fixtures.school();
        school.setStatus(SchoolStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        stubActiveTenant();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginSuspendedTenant() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        Tenant tenant = Fixtures.tenant();
        tenant.setStatus(TenantStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Organization");
    }

    @Test
    void loginMissingTenant() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loginUnknownAccountUsesSameMessageAsWrongPassword() {
        when(userRepository.findByEmailIgnoreCase("x@y.z")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@y.z", "pw")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthService.INVALID_LOGIN);
        verify(auditService).recordAuthFailure("x@y.z", AuthService.INVALID_LOGIN);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void loginMissingSchool() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        stubActiveTenant();
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loginArchivedTenant() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        Tenant tenant = Fixtures.tenant();
        tenant.setStatus(TenantStatus.ARCHIVED);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.of(tenant));
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void loginArchivedSchool() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        School school = Fixtures.school();
        school.setStatus(SchoolStatus.ARCHIVED);
        when(userRepository.findByEmailIgnoreCase("headmaster@example.com")).thenReturn(Optional.of(user));
        stubActiveTenant();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void loginSuperAdminSkipsSchool() {
        User user = Fixtures.user(1L, Role.SUPER_ADMIN);
        when(userRepository.findByEmailIgnoreCase("super_headmaster@example.com")).thenReturn(Optional.of(user));
        stubTokens(user);
        authService.login(new LoginRequest("super_headmaster@example.com", "pw"));
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void registerSchool() {
        when(userRepository.existsByEmailIgnoreCase("yuki.t@example.com")).thenReturn(false);
        when(tenantService.provisionNewOrganization(any())).thenReturn(Fixtures.tenant());
        when(passwordEncoder.encode("HaloCampus1!")).thenReturn("enc");
        User admin = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.save(any(User.class))).thenReturn(admin);
        stubTokens(admin);
        var response = authService.registerSchool(new RegisterSchoolRequest(
                null, "yuki.t@example.com", "HaloCampus1!", "Admin", "07", null, null, "TZ",
                "Chambaka Group", null));
        assertThat(response.accessToken()).isEqualTo("access");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ORGANIZATION_ADMIN);
        assertThat(captor.getValue().getTenantId()).isEqualTo(Fixtures.TENANT_ID);
        assertThat(captor.getValue().getSchoolId()).isNull();
        assertThat(captor.getValue().getCampusId()).isNull();
        verify(auditService).recordAuth(eq(AuditAction.REGISTER_TENANT), eq(admin), anyString());
    }

    @Test
    void registerUsesProvidedTimezoneCurrency() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(tenantService.provisionNewOrganization(any())).thenReturn(Fixtures.tenant());
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenReturn(Fixtures.user(2L, Role.HEADMASTER));
        stubTokens(Fixtures.user(2L, Role.HEADMASTER));
        authService.registerSchool(new RegisterSchoolRequest(
                null, "a@b.com", "HaloCampus1!", "A", null, "UTC", "USD", null, "X", null));
        verify(tenantService).provisionNewOrganization(any());
    }

    @Test
    void registerDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.registerSchool(new RegisterSchoolRequest(
                null, "a@b.com", "HaloCampus1!", "A", null, null, null, null, "X", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> authService.registerSchool(new RegisterSchoolRequest(
                null, "a@b.com", "secret12", "A", null, null, null, null, "X", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least 10");
    }

    @Test
    void registerRejectedInSingleTenant() {
        when(smsProperties.singleTenant()).thenReturn(true);
        assertThatThrownBy(() -> authService.registerSchool(new RegisterSchoolRequest(
                null, "a@b.com", "HaloCampus1!", "A", null, null, null, null, "X", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("single-tenant");
    }

    @Test
    void switchSchoolForbiddenForHeadmaster() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, Role.HEADMASTER)));
        assertThatThrownBy(() -> authService.switchSchool(2L, new SwitchSchoolRequest(1L, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("platform admin");
    }

    @Test
    void switchSchoolAsPlatformAdminWithCampus() {
        User user = Fixtures.user(1L, Role.SUPER_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(campusService.requireInSchool(20L, 1L)).thenReturn(Fixtures.campus());
        stubTokens(user);
        authService.switchSchool(1L, new SwitchSchoolRequest(1L, 20L));
        assertThat(user.getTenantId()).isEqualTo(Fixtures.TENANT_ID);
    }

    @Test
    void switchSchoolForbiddenForTeacher() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(Fixtures.user(3L, Role.TEACHER)));
        assertThatThrownBy(() -> authService.switchSchool(3L, new SwitchSchoolRequest(1L, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void logoutRevokesRefreshToken() {
        RefreshToken stored = new RefreshToken();
        stored.setRevoked(false);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenHash.sha256("rt")))
                .thenReturn(Optional.of(stored));
        authService.logout(new RefreshTokenRequest("rt"));
        assertThat(stored.isRevoked()).isTrue();
        authService.logout(null);
        authService.logout(new RefreshTokenRequest("  "));
    }

    @Test
    void platformAdminIsNeverLockedOut() {
        User admin = Fixtures.user(1L, Role.SUPER_ADMIN);
        admin.setLockedUntil(Instant.now().plusSeconds(3600));
        admin.setFailedLoginAttempts(20);
        when(userRepository.findByEmailIgnoreCase("super_admin@example.com")).thenReturn(Optional.of(admin));
        org.mockito.Mockito.doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        for (int attempt = 0; attempt < AuthService.MAX_FAILED_LOGINS + 3; attempt++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("super_admin@example.com", "pw")))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage(AuthService.INVALID_LOGIN);
        }
        assertThat(admin.getFailedLoginAttempts()).isEqualTo(20);
        assertThat(admin.getLockedUntil()).isAfter(Instant.now());
        verify(loginRateLimitService, org.mockito.Mockito.never()).isBlocked(any());
        verify(loginRateLimitService, org.mockito.Mockito.never()).recordFailure(any());

        org.mockito.Mockito.reset(authenticationManager);
        stubTokens(admin);
        var response = authService.login(new LoginRequest("super_admin@example.com", "pw"));
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(admin.getFailedLoginAttempts()).isZero();
        assertThat(admin.getLockedUntil()).isNull();
    }

    @Test
    void loginBlockedByRateLimit() {
        when(loginRateLimitService.isBlocked("headmaster@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.login(new LoginRequest("headmaster@example.com", "pw")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    void refreshSuccess() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        stored.setRevoked(false);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenHash.sha256("rt")))
                .thenReturn(Optional.of(stored));
        stubTokens(user);
        var response = authService.refresh(new RefreshTokenRequest("rt"));
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refreshExpired() {
        RefreshToken stored = new RefreshToken();
        stored.setExpiresAt(Instant.now().minusSeconds(5));
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("rt")))
                .isInstanceOf(ApiException.class);
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refreshInvalid() {
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("rt")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void refreshDisabledUser() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setEnabled(false);
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString())).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("rt")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void meAndChangePassword() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userMapper.toProfile(user)).thenReturn(Fixtures.profile(user));
        assertThat(authService.me(2L).email()).isEqualTo(user.getEmail());

        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("HaloCampus1!")).thenReturn("newhash");
        authService.changePassword(2L, new ChangePasswordRequest("old", "HaloCampus1!"));
        assertThat(user.getPassword()).isEqualTo("newhash");
        verify(refreshTokenRepository).deleteByUserId(2L);
    }

    @Test
    void changePasswordWrongCurrent() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("no", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> authService.changePassword(2L, new ChangePasswordRequest("no", "HaloCampus1!")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changePasswordRejectsWeakPassword() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        assertThatThrownBy(() -> authService.changePassword(2L, new ChangePasswordRequest("old", "newpass12")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void meMissing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.me(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePasswordMissingUser() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.changePassword(9L, new ChangePasswordRequest("a", "bbbbbbbb")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void switchSchoolRejectsArchivedSchool() {
        User user = Fixtures.user(1L, Role.SUPER_ADMIN);
        School school = Fixtures.school();
        school.setStatus(SchoolStatus.ARCHIVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> authService.switchSchool(1L, new SwitchSchoolRequest(1L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void switchSchoolMissingUser() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.switchSchool(9L, new SwitchSchoolRequest(1L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void headmasterUnlocksLockedAccount() {
        User student = Fixtures.user(4L, Role.STUDENT);
        student.setFailedLoginAttempts(3);
        student.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findByIdAndSchoolId(4L, 1L)).thenReturn(Optional.of(student));

        var unlocked = authService.unlock(4L, null, Fixtures.principal(Role.HEADMASTER));

        assertThat(student.getFailedLoginAttempts()).isZero();
        assertThat(student.getLockedUntil()).isNull();
        assertThat(unlocked.userId()).isEqualTo(4L);
        assertThat(unlocked.lockedUntil()).isNull();
        verify(auditService).record(any());
    }

    @Test
    void academicMasterCannotUnlockAnotherSchool() {
        assertThatThrownBy(() -> authService.unlock(4L, 99L, Fixtures.principal(Role.ACADEMIC_MASTER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("your school");
        verify(userRepository, never()).findByIdAndSchoolId(any(), any());
    }

    @Test
    void teacherCannotUnlockAccounts() {
        assertThatThrownBy(() -> authService.unlock(4L, 1L, Fixtures.principal(Role.TEACHER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("headmaster or school admin");
    }

    @Test
    void headmasterResetsTwoFactor() {
        User student = Fixtures.user(4L, Role.STUDENT);
        student.setTotpEnabled(true);
        student.setTotpSecret("EXISTINGSECRET");
        when(userRepository.findByIdAndSchoolId(4L, 1L)).thenReturn(Optional.of(student));
        doAnswer(invocation -> {
            User target = invocation.getArgument(0);
            target.setTotpSecret(null);
            target.setTotpEnabled(false);
            return null;
        }).when(twoFactorService).reset(student);

        var reset = authService.resetTwoFactor(4L, null, Fixtures.principal(Role.HEADMASTER));

        assertThat(student.isTotpEnabled()).isFalse();
        assertThat(student.getTotpSecret()).isNull();
        assertThat(reset.userId()).isEqualTo(4L);
        assertThat(reset.totpEnabled()).isFalse();
        verify(auditService).record(any());
    }

    @Test
    void academicMasterCannotResetTwoFactorAtAnotherSchool() {
        assertThatThrownBy(() -> authService.resetTwoFactor(4L, 99L, Fixtures.principal(Role.ACADEMIC_MASTER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("your school");
        verify(twoFactorService, never()).reset(any());
    }

    @Test
    void schoolOfficerCannotResetSuperAdminTwoFactor() {
        User platform = Fixtures.user(1L, Role.SUPER_ADMIN);
        platform.setSchoolId(1L);
        when(userRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(platform));
        assertThatThrownBy(() -> authService.resetTwoFactor(1L, 1L, Fixtures.principal(Role.HEADMASTER)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be reset");
        verify(twoFactorService, never()).reset(any());
    }

    @Test
    void userResetsOwnTwoFactor() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setTotpEnabled(true);
        user.setTotpSecret("EXISTINGSECRET");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            User target = invocation.getArgument(0);
            target.setTotpSecret(null);
            target.setTotpEnabled(false);
            return null;
        }).when(twoFactorService).reset(user);

        var reset = authService.resetOwnTwoFactor(2L);

        assertThat(reset.totpEnabled()).isFalse();
        verify(auditService).recordAuth(eq(AuditAction.TWO_FACTOR_RESET), eq(user), any());
    }

    @Test
    void headmasterResetsPasswordAndSmsesTemporary() {
        User student = Fixtures.user(4L, Role.STUDENT);
        when(userRepository.findByIdAndSchoolId(4L, 1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-new");
        when(credentialSms.sendTemporaryPassword(eq(student), anyString())).thenReturn(SmsSendResult.ok("ref"));

        var result = authService.resetPassword(4L, null, Fixtures.principal(Role.HEADMASTER), new AdminResetPasswordRequest(null));

        assertThat(student.getPassword()).isEqualTo("hashed-new");
        assertThat(student.getFailedLoginAttempts()).isZero();
        assertThat(result.smsSent()).isTrue();
        verify(refreshTokenRepository).deleteByUserId(4L);
        verify(auditService).record(any());
    }

    @Test
    void teacherCannotResetPassword() {
        assertThatThrownBy(() -> authService.resetPassword(4L, 1L, Fixtures.principal(Role.TEACHER), null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("headmaster or school admin");
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void headmasterListsSchoolUsers() {
        User student = Fixtures.user(4L, Role.STUDENT);
        when(userRepository.findBySchoolIdAndRoleNot(eq(1L), eq(Role.SUPER_ADMIN), any()))
                .thenReturn(new PageImpl<>(List.of(student)));

        var page = authService.listSchoolUsers(null, null, Fixtures.principal(Role.HEADMASTER), PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).role()).isEqualTo(Role.STUDENT);
    }

    @Test
    void teacherCannotListSchoolUsers() {
        assertThatThrownBy(() -> authService.listSchoolUsers(1L, null, Fixtures.principal(Role.TEACHER), PageRequest.of(0, 20)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("headmaster or school admin");
    }

    @Test
    void superAdminListsAllPlatformUsers() {
        User student = Fixtures.user(4L, Role.STUDENT);
        User platform = Fixtures.user(1L, Role.SUPER_ADMIN);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(student, platform)));
        when(schoolRepository.findAllById(any())).thenReturn(List.of(Fixtures.school()));
        when(tenantRepository.findAllById(any())).thenReturn(List.of(Fixtures.tenant()));

        var page = authService.listSchoolUsers(null, null, Fixtures.principal(Role.SUPER_ADMIN), PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).schoolName()).isEqualTo("Chambaka Secondary");
        assertThat(page.content().get(0).tenantName()).isEqualTo("Chambaka Group");
        assertThat(page.content().get(1).role()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(page.content().get(1).schoolId()).isNull();
        verify(tenantService, never()).requireSchoolInTenant(any(), any());
    }

    @Test
    void superAdminResetsPasswordWithoutSchoolId() {
        User student = Fixtures.user(4L, Role.STUDENT);
        when(userRepository.findById(4L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-new");
        when(credentialSms.sendTemporaryPassword(eq(student), anyString())).thenReturn(SmsSendResult.ok("ref"));

        var result = authService.resetPassword(4L, null, Fixtures.principal(Role.SUPER_ADMIN), new AdminResetPasswordRequest(null));

        assertThat(result.smsSent()).isTrue();
        assertThat(student.getPassword()).isEqualTo("hashed-new");
        verify(userRepository).findById(4L);
        verify(userRepository, never()).findByIdAndSchoolId(any(), any());
        verify(refreshTokenRepository).deleteByUserId(4L);
    }

    private void stubActiveTenant() {
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.of(Fixtures.tenant()));
    }

    private void stubTokens(User user) {
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshTokenValue()).thenReturn("refresh");
        when(jwtService.refreshExpiry()).thenReturn(Instant.now().plusSeconds(60));
        when(jwtService.accessTokenTtlSeconds()).thenReturn(3600L);
        when(userMapper.toProfile(any(User.class))).thenReturn(Fixtures.profile(user));
    }
}
