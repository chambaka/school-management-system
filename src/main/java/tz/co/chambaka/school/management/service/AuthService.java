package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.admin.SchoolUserResponse;
import tz.co.chambaka.school.management.dto.auth.AdminResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.AdminResetPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.AuthResponse;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.LoginResponse;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.ResetTwoFactorResponse;
import tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.UnlockAccountResponse;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.dto.auth.VerifyTwoFactorRequest;
import tz.co.chambaka.school.management.dto.common.PageResponse;
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
import tz.co.chambaka.school.management.audit.AuditEventDraft;
import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.security.JwtService;
import tz.co.chambaka.school.management.security.PasswordPolicy;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.ratelimit.LoginRateLimitService;
import tz.co.chambaka.school.management.sms.CredentialSmsService;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import tz.co.chambaka.school.management.sms.SmsSendResult;
import tz.co.chambaka.school.management.util.TokenHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    public static final String INVALID_LOGIN = "Login or password is incorrect";
    public static final int MAX_FAILED_LOGINS = 5;
    public static final long LOCKOUT_SECONDS = 15L * 60L;

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
    private final TwoFactorService twoFactorService;
    private final CredentialSmsService credentialSms;
    private final LoginRateLimitService loginRateLimitService;

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
            SmsProperties smsProperties,
            TwoFactorService twoFactorService,
            CredentialSmsService credentialSms,
            LoginRateLimitService loginRateLimitService
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
        this.twoFactorService = twoFactorService;
        this.credentialSms = credentialSms;
        this.loginRateLimitService = loginRateLimitService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String key = request.loginKey();
        if (key.isBlank()) {
            throw new BusinessException("Enter your email, phone, or username");
        }
        User lookup = findByIdentifier(key);
        boolean platformAdmin = isPlatformAdmin(lookup);
        if (!platformAdmin && loginRateLimitService != null && loginRateLimitService.isBlocked(key)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts. Try again later.");
        }
        if (lookup == null) {
            log.warn("Login failed for unknown identifier={}", key);
            auditService.recordAuthFailure(key, INVALID_LOGIN);
            throw new BadCredentialsException(INVALID_LOGIN);
        }
        if (!platformAdmin && lookup.getLockedUntil() != null && lookup.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.LOCKED, "Account is locked. Try again later.");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(lookup.getEmail(), request.password()));
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            log.warn("Login failed for identifier={}", key);
            auditService.recordAuthFailure(key, INVALID_LOGIN);
            if (!platformAdmin) {
                if (loginRateLimitService != null) {
                    loginRateLimitService.recordFailure(key);
                }
                lookup.setFailedLoginAttempts(lookup.getFailedLoginAttempts() + 1);
                if (lookup.getFailedLoginAttempts() >= MAX_FAILED_LOGINS) {
                    lookup.setLockedUntil(Instant.now().plusSeconds(LOCKOUT_SECONDS));
                    lookup.setFailedLoginAttempts(0);
                    log.warn("Locked account userId={} for failed attempts", lookup.getId());
                    throw new ApiException(HttpStatus.LOCKED, "Account is locked after too many failed attempts");
                }
            }
            throw new BadCredentialsException(INVALID_LOGIN, ex);
        }
        User user = lookup;
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (loginRateLimitService != null) {
            loginRateLimitService.clear(key);
        }
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
        School school = null;
        if (user.getSchoolId() != null) {
            school = schoolRepository.findById(user.getSchoolId())
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
        if (twoFactorService.isRequired(school)) {
            return twoFactorService.startChallenge(user);
        }
        return completeLogin(user);
    }

    @Transactional
    public LoginResponse verifyTwoFactor(VerifyTwoFactorRequest request) {
        User user = twoFactorService.verify(request);
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        return completeLogin(user);
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
                .username(request.adminEmail().substring(0, request.adminEmail().indexOf('@')).toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.HEADMASTER)
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
        if (!user.getRole().switchesSchool()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the headmaster or platform admin can switch school");
        }
        School school;
        if (user.getRole() == Role.HEADMASTER || user.getRole() == Role.SCHOOL_ADMIN) {
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
    public void logout(RefreshTokenRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        String hash = TokenHash.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash).ifPresent(token -> token.setRevoked(true));
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

    @Transactional
    public UnlockAccountResponse unlock(Long userId, Long schoolId, UserPrincipal actor) {
        if (actor == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sign in required");
        }
        User user = requireUnlockTarget(userId, schoolId, actor);
        if (user.getRole() == Role.SUPER_ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account cannot be unlocked here");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        log.info("Unlocked login userId={} email={} by userId={} role={}",
                user.getId(), user.getEmail(), actor.getId(), actor.getRole());
        auditService.record(new AuditEventDraft()
                .scope(actor.getRole() == Role.SUPER_ADMIN ? AuditScope.PLATFORM : AuditScope.TENANT)
                .action(AuditAction.ACCOUNT_UNLOCKED)
                .schoolId(user.getSchoolId())
                .resourceType("User")
                .resourceId(String.valueOf(user.getId()))
                .summary("Unlocked login for " + user.getEmail())
                .details("targetUserId=" + user.getId()
                        + " targetRole=" + user.getRole()
                        + " actorUserId=" + actor.getId()
                        + " actorRole=" + actor.getRole())
                .httpMethod("POST")
                .httpPath("/api/v1/users/" + userId + "/unlock")
                .statusCode(200));
        return new UnlockAccountResponse(user.getId(), user.getName(), user.getEmail(), null);
    }

    @Transactional
    public ResetTwoFactorResponse resetTwoFactor(Long userId, Long schoolId, UserPrincipal actor) {
        if (actor == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sign in required");
        }
        User user = requireUnlockTarget(userId, schoolId, actor);
        if (user.getRole() == Role.SUPER_ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account cannot be reset here");
        }
        twoFactorService.reset(user);
        log.info("Reset 2FA userId={} email={} by userId={} role={}",
                user.getId(), user.getEmail(), actor.getId(), actor.getRole());
        auditService.record(new AuditEventDraft()
                .scope(actor.getRole() == Role.SUPER_ADMIN ? AuditScope.PLATFORM : AuditScope.TENANT)
                .action(AuditAction.TWO_FACTOR_RESET)
                .schoolId(user.getSchoolId())
                .resourceType("User")
                .resourceId(String.valueOf(user.getId()))
                .summary("Reset ShuleHub 2FA for " + user.getEmail())
                .details("targetUserId=" + user.getId()
                        + " targetRole=" + user.getRole()
                        + " actorUserId=" + actor.getId()
                        + " actorRole=" + actor.getRole())
                .httpMethod("POST")
                .httpPath("/api/v1/users/" + userId + "/reset-2fa")
                .statusCode(200));
        return new ResetTwoFactorResponse(user.getId(), user.getName(), user.getEmail(), user.isTotpEnabled());
    }

    @Transactional
    public ResetTwoFactorResponse resetOwnTwoFactor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        twoFactorService.reset(user);
        log.info("Reset own 2FA userId={}", userId);
        auditService.recordAuth(AuditAction.TWO_FACTOR_RESET, user, "Reset own ShuleHub 2FA");
        return new ResetTwoFactorResponse(user.getId(), user.getName(), user.getEmail(), user.isTotpEnabled());
    }

    @Transactional(readOnly = true)
    public PageResponse<SchoolUserResponse> listSchoolUsers(
            Long schoolId,
            Role role,
            UserPrincipal actor,
            Pageable pageable
    ) {
        if (actor == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sign in required");
        }
        Page<User> page;
        if (actor.getRole() == Role.SUPER_ADMIN && schoolId == null) {
            page = role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
        } else {
            Long scopedSchoolId = requireManagedSchoolId(schoolId, actor);
            page = role == null
                    ? userRepository.findBySchoolIdAndRoleNot(scopedSchoolId, Role.SUPER_ADMIN, pageable)
                    : userRepository.findBySchoolIdAndRole(scopedSchoolId, role, pageable);
        }
        Map<Long, String> schoolNames = schoolNames(page);
        Map<Long, String> tenantNames = tenantNames(page);
        return PageResponse.of(page.map(user -> toSchoolUser(user, schoolNames, tenantNames)));
    }

    @Transactional
    public AdminResetPasswordResponse resetPassword(Long userId, Long schoolId, UserPrincipal actor,
                                                    AdminResetPasswordRequest request) {
        if (actor == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sign in required");
        }
        User user = requireUnlockTarget(userId, schoolId, actor);
        if (user.getRole() == Role.SUPER_ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account cannot be reset here");
        }
        String provided = request == null ? null : request.password();
        boolean generate = provided == null || provided.isBlank();
        if (generate && (user.getPhone() == null || user.getPhone().isBlank())) {
            throw new BusinessException("Add a phone number to send a temporary password, or set a password.");
        }
        if (!generate) {
            PasswordPolicy.requireValid(provided, user.getEmail(), user.getName());
        }
        String password = generate ? PasswordPolicy.generateTemporary() : provided;
        user.setPassword(passwordEncoder.encode(password));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        refreshTokenRepository.deleteByUserId(user.getId());
        SmsSendResult sms = credentialSms.sendTemporaryPassword(user, password);
        boolean smsSent = sms != null && sms.sent();
        log.info("Admin reset password userId={} email={} by userId={} role={} smsSent={}",
                user.getId(), user.getEmail(), actor.getId(), actor.getRole(), smsSent);
        auditService.record(new AuditEventDraft()
                .scope(actor.getRole() == Role.SUPER_ADMIN ? AuditScope.PLATFORM : AuditScope.TENANT)
                .action(AuditAction.PASSWORD_RESET)
                .schoolId(user.getSchoolId())
                .resourceType("User")
                .resourceId(String.valueOf(user.getId()))
                .summary("Reset password for " + user.getEmail())
                .details("targetUserId=" + user.getId()
                        + " targetRole=" + user.getRole()
                        + " actorUserId=" + actor.getId()
                        + " actorRole=" + actor.getRole()
                        + " smsSent=" + smsSent)
                .httpMethod("POST")
                .httpPath("/api/v1/users/" + userId + "/reset-password")
                .statusCode(200));
        return new AdminResetPasswordResponse(user.getId(), user.getName(), user.getEmail(), smsSent);
    }

    private SchoolUserResponse toSchoolUser(User user, Map<Long, String> schoolNames, Map<Long, String> tenantNames) {
        Long schoolId = user.getSchoolId();
        Long tenantId = user.getTenantId();
        return new SchoolUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.activeLockedUntil(),
                user.isTotpEnabled(),
                user.getLastLoginAt(),
                schoolId,
                schoolId == null ? null : schoolNames.get(schoolId),
                tenantId,
                tenantId == null ? null : tenantNames.get(tenantId));
    }

    private Map<Long, String> schoolNames(Page<User> page) {
        Set<Long> ids = new HashSet<>();
        for (User user : page.getContent()) {
            if (user.getSchoolId() != null) {
                ids.add(user.getSchoolId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (School school : schoolRepository.findAllById(ids)) {
            names.put(school.getId(), school.getName());
        }
        return names;
    }

    private Map<Long, String> tenantNames(Page<User> page) {
        Set<Long> ids = new HashSet<>();
        for (User user : page.getContent()) {
            if (user.getTenantId() != null) {
                ids.add(user.getTenantId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (Tenant tenant : tenantRepository.findAllById(ids)) {
            names.put(tenant.getId(), tenant.getName());
        }
        return names;
    }

    private Long requireManagedSchoolId(Long schoolId, UserPrincipal actor) {
        if (actor == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sign in required");
        }
        Long scopedSchoolId = schoolId != null ? schoolId : actor.getSchoolId();
        return switch (actor.getRole()) {
            case SUPER_ADMIN -> {
                if (scopedSchoolId == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "schoolId is required");
                }
                yield scopedSchoolId;
            }
            case HEADMASTER -> {
                if (scopedSchoolId == null) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Select a school first");
                }
                tenantService.requireSchoolInTenant(actor.getTenantId(), scopedSchoolId);
                yield scopedSchoolId;
            }
            case ACADEMIC_MASTER -> {
                if (actor.getSchoolId() == null || (scopedSchoolId != null && !scopedSchoolId.equals(actor.getSchoolId()))) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage accounts at your school");
                }
                yield actor.getSchoolId();
            }
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "Only a headmaster or school admin can manage school users");
        };
    }

    private User requireUnlockTarget(Long userId, Long schoolId, UserPrincipal actor) {
        Long scopedSchoolId = schoolId != null ? schoolId : actor.getSchoolId();
        return switch (actor.getRole()) {
            case SUPER_ADMIN -> {
                if (scopedSchoolId != null) {
                    yield userRepository.findByIdAndSchoolId(userId, scopedSchoolId)
                            .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
                }
                yield userRepository.findById(userId)
                        .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
            }
            case HEADMASTER -> {
                if (scopedSchoolId == null) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Select a school first");
                }
                tenantService.requireSchoolInTenant(actor.getTenantId(), scopedSchoolId);
                yield userRepository.findByIdAndSchoolId(userId, scopedSchoolId)
                        .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
            }
            case ACADEMIC_MASTER -> {
                if (actor.getSchoolId() == null || (scopedSchoolId != null && !scopedSchoolId.equals(actor.getSchoolId()))) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "You can only unlock accounts at your school");
                }
                yield userRepository.findByIdAndSchoolId(userId, actor.getSchoolId())
                        .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
            }
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "Only a headmaster or school admin can unlock accounts");
        };
    }

    private LoginResponse completeLogin(User user) {
        user.setLastLoginAt(Instant.now());
        log.info("Login succeeded userId={} role={} tenantId={} schoolId={}",
                user.getId(), user.getRole(), user.getTenantId(), user.getSchoolId());
        auditService.recordAuth(AuditAction.LOGIN, user, "User logged in");
        return LoginResponse.authenticated(issueTokens(user));
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

    private static boolean isPlatformAdmin(User user) {
        return user != null && user.getRole() == Role.SUPER_ADMIN;
    }

    private User findByIdentifier(String raw) {
        if (raw.contains("@")) {
            return userRepository.findByEmailIgnoreCase(raw).orElse(null);
        }
        User byUsername = userRepository.findByUsernameIgnoreCase(raw).orElse(null);
        if (byUsername != null) {
            return byUsername;
        }
        String phone = PhoneNumbers.persist(raw);
        if (phone != null) {
            return userRepository.findFirstByPhone(phone).orElse(null);
        }
        return userRepository.findByEmailIgnoreCase(raw).orElse(null);
    }
}
