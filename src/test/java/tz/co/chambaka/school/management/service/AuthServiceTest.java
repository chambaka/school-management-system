package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.UserMapper;
import tz.co.chambaka.school.management.model.RefreshToken;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.repository.RefreshTokenRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.security.JwtService;
import tz.co.chambaka.school.management.support.Fixtures;
import tz.co.chambaka.school.management.util.TokenHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginSuccess() {
        User user = Fixtures.user(2L, Role.ADMIN);
        School school = Fixtures.school();
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        stubTokens(user);
        var response = authService.login(new LoginRequest("admin@example.com", "pw"));
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(authenticationManager).authenticate(any());
        verify(auditService).recordAuth(AuditAction.LOGIN, user, "User logged in");
    }

    @Test
    void loginFailedIsAudited() {
        org.mockito.Mockito.doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@example.com", "pw")))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditService).recordAuthFailure("admin@example.com", "Invalid email or password");
    }

    @Test
    void loginDisabled() {
        User user = Fixtures.user(2L, Role.ADMIN);
        user.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@example.com", "pw")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginSuspendedSchool() {
        User user = Fixtures.user(2L, Role.ADMIN);
        School school = Fixtures.school();
        school.setStatus(SchoolStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@example.com", "pw")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void loginMissingUserAfterAuth() {
        when(userRepository.findByEmailIgnoreCase("x@y.z")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@y.z", "pw")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loginMissingSchool() {
        User user = Fixtures.user(2L, Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@example.com", "pw")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loginSuperAdminSkipsSchool() {
        User user = Fixtures.user(1L, Role.SUPER_ADMIN);
        when(userRepository.findByEmailIgnoreCase("super_admin@example.com")).thenReturn(Optional.of(user));
        stubTokens(user);
        authService.login(new LoginRequest("super_admin@example.com", "pw"));
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void registerSchool() {
        when(userRepository.existsByEmailIgnoreCase("yuki.t@example.com")).thenReturn(false);
        when(schoolRepository.existsBySlug("chambaka-secondary")).thenReturn(true, false);
        School saved = Fixtures.school();
        when(schoolRepository.save(any(School.class))).thenReturn(saved);
        when(passwordEncoder.encode("secret12")).thenReturn("enc");
        User admin = Fixtures.user(2L, Role.ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(admin);
        stubTokens(admin);
        var response = authService.registerSchool(new RegisterSchoolRequest(
                "Chambaka Secondary", "yuki.t@example.com", "secret12", "Admin", "07", null, null, "TZ"));
        assertThat(response.accessToken()).isEqualTo("access");
        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        assertThat(captor.getValue().getTimezone()).isEqualTo("Africa/Dar_es_Salaam");
        assertThat(captor.getValue().getCurrency()).isEqualTo("TZS");
    }

    @Test
    void registerUsesProvidedTimezoneCurrency() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(schoolRepository.existsBySlug(anyString())).thenReturn(false);
        when(schoolRepository.save(any(School.class))).thenReturn(Fixtures.school());
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenReturn(Fixtures.user(2L, Role.ADMIN));
        stubTokens(Fixtures.user(2L, Role.ADMIN));
        authService.registerSchool(new RegisterSchoolRequest(
                "X", "a@b.com", "secret12", "A", null, "UTC", "USD", null));
        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        assertThat(captor.getValue().getTimezone()).isEqualTo("UTC");
        assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
    }

    @Test
    void registerDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.registerSchool(new RegisterSchoolRequest(
                "X", "a@b.com", "secret12", "A", null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void refreshSuccess() {
        User user = Fixtures.user(2L, Role.ADMIN);
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
        User user = Fixtures.user(2L, Role.ADMIN);
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
        User user = Fixtures.user(2L, Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userMapper.toProfile(user)).thenReturn(Fixtures.profile(user));
        assertThat(authService.me(2L).email()).isEqualTo(user.getEmail());

        when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpass12")).thenReturn("newhash");
        authService.changePassword(2L, new ChangePasswordRequest("old", "newpass12"));
        assertThat(user.getPassword()).isEqualTo("newhash");
        verify(refreshTokenRepository).deleteByUserId(2L);
    }

    @Test
    void changePasswordWrongCurrent() {
        User user = Fixtures.user(2L, Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("no", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> authService.changePassword(2L, new ChangePasswordRequest("no", "newpass12")))
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

    private void stubTokens(User user) {
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshTokenValue()).thenReturn("refresh");
        when(jwtService.refreshExpiry()).thenReturn(Instant.now().plusSeconds(60));
        when(jwtService.accessTokenTtlSeconds()).thenReturn(3600L);
        when(userMapper.toProfile(any(User.class))).thenReturn(Fixtures.profile(user));
    }
}
