package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.AuthResponse;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.UserMapper;
import tz.co.chambaka.school.management.model.Campus;
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
import tz.co.chambaka.school.management.security.PasswordPolicy;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
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

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final CampusService campusService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final SmsProperties smsProperties;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            TenantRepository tenantRepository,
            TenantService tenantService,
            CampusService campusService,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper,
            AuditService auditService,
            SmsProperties smsProperties
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
        this.campusService = campusService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditService = auditService;
        this.smsProperties = smsProperties;
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
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
            if (tenant.getStatus() == TenantStatus.SUSPENDED || tenant.getStatus() == TenantStatus.ARCHIVED) {
                log.warn("Login blocked tenant status={} tenantId={} userId={}",
                        tenant.getStatus(), tenant.getId(), user.getId());
                throw new ApiException(HttpStatus.FORBIDDEN,
                        tenant.getStatus() == TenantStatus.ARCHIVED
                                ? "Organization account is no longer available"
                                : "Organization account is suspended");
            }
        }
        if (user.getSchoolId() != null) {
            School school = schoolRepository.findById(user.getSchoolId())
                    .orElseThrow(() -> new ResourceNotFoundException("School not found"));
            if (school.getStatus() == SchoolStatus.SUSPENDED || school.getStatus() == SchoolStatus.ARCHIVED) {
                log.warn("Login blocked school status={} schoolId={} userId={}",
                        school.getStatus(), school.getId(), user.getId());
                throw new ApiException(HttpStatus.FORBIDDEN,
                        school.getStatus() == SchoolStatus.ARCHIVED
                                ? "School account is no longer available"
                                : "School account is suspended");
            }
        }
        user.setLastLoginAt(Instant.now());
        log.info("Login succeeded userId={} role={} tenantId={} schoolId={}",
                user.getId(), user.getRole(), user.getTenantId(), user.getSchoolId());
        auditService.recordAuth(AuditAction.LOGIN, user, "User logged in");
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse registerSchool(RegisterSchoolRequest request) {
        if (smsProperties.singleTenant()) {
            throw new BusinessException("Organization self-registration is disabled in single-tenant mode");
        }
        PasswordPolicy.requireValid(request.password(), request.adminEmail(), request.adminName());
        if (userRepository.existsByEmailIgnoreCase(request.adminEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        Tenant tenant = tenantService.provisionNewOrganization(request);
        User admin = User.builder()
                .tenantId(tenant.getId())
                .name(request.adminName())
                .email(request.adminEmail().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.TENANT_ADMIN)
                .phone(PhoneNumbers.persist(request.phone()))
                .enabled(true)
                .build();
        admin = userRepository.save(admin);
        log.info("Registered tenant id={} slug={} adminUserId={}", tenant.getId(), tenant.getSlug(), admin.getId());
        auditService.recordAuth(AuditAction.REGISTER_TENANT, admin,
                "Registered organization " + tenant.getName());
        return issueTokens(admin);
    }

    @Transactional
    public AuthResponse switchSchool(Long userId, SwitchSchoolRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if (user.getRole() != Role.TENANT_ADMIN && user.getRole() != Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only tenant or platform admins can switch school");
        }
        School school;
        if (user.getRole() == Role.TENANT_ADMIN) {
            school = tenantService.requireSchoolInTenant(user.getTenantId(), request.schoolId());
        } else {
            school = schoolRepository.findById(request.schoolId())
                    .orElseThrow(() -> ResourceNotFoundException.of("School", request.schoolId()));
        }
        if (school.getStatus() == SchoolStatus.ARCHIVED) {
            throw ResourceNotFoundException.of("School", request.schoolId());
        }
        Campus campus;
        if (request.campusId() != null) {
            campus = campusService.requireInSchool(request.campusId(), school.getId());
        } else {
            campus = campusService.requirePrimary(school.getId());
        }
        user.setTenantId(school.getTenantId());
        user.setSchoolId(school.getId());
        user.setCampusId(campus.getId());
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Switched active school userId={} schoolId={} campusId={}", userId, school.getId(), campus.getId());
        return issueTokens(user);
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
        PasswordPolicy.requireValid(request.newPassword(), user.getEmail(), user.getName());
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
}
