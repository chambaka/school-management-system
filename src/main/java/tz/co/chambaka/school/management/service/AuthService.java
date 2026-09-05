package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.auth.AuthResponse;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
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
import tz.co.chambaka.school.management.util.SlugUtil;
import tz.co.chambaka.school.management.util.TokenHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper,
            AuditService auditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for email={}", request.email());
            auditService.recordAuthFailure(request.email(), "Invalid email or password");
            throw ex;
        }
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isEnabled()) {
            log.warn("Login blocked disabled account userId={}", user.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        if (user.getSchoolId() != null) {
            School school = schoolRepository.findById(user.getSchoolId())
                    .orElseThrow(() -> new ResourceNotFoundException("School not found"));
            if (school.getStatus() == SchoolStatus.SUSPENDED) {
                log.warn("Login blocked suspended school schoolId={} userId={}", school.getId(), user.getId());
                throw new ApiException(HttpStatus.FORBIDDEN, "School account is suspended");
            }
        }
        user.setLastLoginAt(Instant.now());
        log.info("Login succeeded userId={} role={} schoolId={}", user.getId(), user.getRole(), user.getSchoolId());
        auditService.recordAuth(AuditAction.LOGIN, user, "User logged in");
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse registerSchool(RegisterSchoolRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.adminEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        School school = new School();
        school.setName(request.schoolName());
        school.setSlug(uniqueSlug(request.schoolName()));
        school.setEmail(request.adminEmail());
        school.setPhone(request.phone());
        school.setTimezone(request.timezone() != null ? request.timezone() : "Africa/Dar_es_Salaam");
        school.setCurrency(request.currency() != null ? request.currency() : "TZS");
        school.setCountry(request.country());
        school.setStatus(SchoolStatus.TRIAL);
        school.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        school = schoolRepository.save(school);

        User admin = User.builder()
                .schoolId(school.getId())
                .name(request.adminName())
                .email(request.adminEmail().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .phone(request.phone())
                .enabled(true)
                .build();
        admin = userRepository.save(admin);
        log.info("Registered school id={} slug={} adminUserId={}", school.getId(), school.getSlug(), admin.getId());
        auditService.recordAuth(AuditAction.REGISTER_SCHOOL, admin,
                "Registered school " + school.getName() + " (" + school.getSlug() + ")");
        return issueTokens(admin);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = TokenHash.sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        stored.setRevoked(true);
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        log.info("Refresh token rotated userId={}", user.getId());
        auditService.recordAuth(AuditAction.TOKEN_REFRESH, user, "Refresh token rotated");
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return userMapper.toProfile(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Password changed userId={}", userId);
        auditService.recordAuth(AuditAction.PASSWORD_CHANGE, user, "Password changed");
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user);
        String refreshValue = jwtService.generateRefreshTokenValue();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHash.sha256(refreshValue));
        refreshToken.setExpiresAt(jwtService.refreshExpiry());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(access, refreshValue, jwtService.accessTokenTtlSeconds(), "Bearer",
                userMapper.toProfile(user));
    }

    private String uniqueSlug(String name) {
        String base = SlugUtil.slugify(name);
        String slug = base;
        int attempt = 0;
        while (schoolRepository.existsBySlug(slug)) {
            attempt++;
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (attempt > 8) {
                break;
            }
        }
        return slug;
    }
}
