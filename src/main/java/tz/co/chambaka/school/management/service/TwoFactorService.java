package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditEventDraft;
import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.dto.auth.LoginResponse;
import tz.co.chambaka.school.management.dto.auth.SchoolTwoFactorResponse;
import tz.co.chambaka.school.management.dto.auth.TwoFactorSettingsResponse;
import tz.co.chambaka.school.management.dto.auth.VerifyTwoFactorRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.TwoFactorChallenge;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.repository.TwoFactorChallengeRepository;
import tz.co.chambaka.school.management.security.TotpService;
import tz.co.chambaka.school.management.util.TokenHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TwoFactorService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorService.class);
    public static final String INVALID_CODE = "Invalid authenticator code";
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration CHALLENGE_TTL = Duration.ofMinutes(10);

    private final SchoolRepository schoolRepository;
    private final TenantRepository tenantRepository;
    private final TwoFactorChallengeRepository challengeRepository;
    private final TotpService totpService;
    private final AuditService auditService;

    public TwoFactorService(
            SchoolRepository schoolRepository,
            TenantRepository tenantRepository,
            TwoFactorChallengeRepository challengeRepository,
            TotpService totpService,
            AuditService auditService
    ) {
        this.schoolRepository = schoolRepository;
        this.tenantRepository = tenantRepository;
        this.challengeRepository = challengeRepository;
        this.totpService = totpService;
        this.auditService = auditService;
    }

    public boolean isRequired(School school) {
        return school != null && school.isTwoFactorEnabled();
    }

    @Transactional(readOnly = true)
    public TwoFactorSettingsResponse settings() {
        Map<Long, String> tenantNames = tenantRepository.findAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        List<SchoolTwoFactorResponse> schools = schoolRepository.findAll(Sort.by("name")).stream()
                .map(school -> toResponse(school, tenantNames.get(school.getTenantId())))
                .toList();
        return new TwoFactorSettingsResponse(
                TotpService.ISSUER,
                "Require Google Authenticator at sign-in for users of a school. Only a platform admin can change this.",
                schools);
    }

    @Transactional
    public SchoolTwoFactorResponse updateEnabled(Long schoolId, boolean enabled) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("School", schoolId));
        school.setTwoFactorEnabled(enabled);
        String tenantName = school.getTenantId() == null ? null
                : tenantRepository.findById(school.getTenantId()).map(Tenant::getName).orElse(null);
        log.info("ShuleHub 2FA schoolId={} {}", schoolId, enabled ? "enabled" : "disabled");
        auditService.record(new AuditEventDraft()
                .scope(AuditScope.PLATFORM)
                .action(AuditAction.TWO_FACTOR_TOGGLED)
                .schoolId(schoolId)
                .resourceType("School")
                .resourceId(String.valueOf(schoolId))
                .summary((enabled ? "ShuleHub 2FA enabled" : "ShuleHub 2FA disabled") + " for " + school.getName())
                .httpMethod("PUT")
                .httpPath("/api/v1/platform/two-factor/" + schoolId)
                .statusCode(200));
        return toResponse(school, tenantName);
    }

    @Transactional
    public LoginResponse startChallenge(User user) {
        challengeRepository.deleteByUserId(user.getId());
        boolean enrolled = user.isTotpEnabled() && user.getTotpSecret() != null && !user.getTotpSecret().isBlank();
        String pendingSecret = enrolled ? null : totpService.newSecret();
        String rawToken = UUID.randomUUID().toString();
        TwoFactorChallenge challenge = new TwoFactorChallenge();
        challenge.setUser(user);
        challenge.setTokenHash(TokenHash.sha256(rawToken));
        challenge.setPendingSecret(pendingSecret);
        challenge.setExpiresAt(Instant.now().plus(CHALLENGE_TTL));
        challenge.setConsumed(false);
        challenge.setFailedAttempts(0);
        challengeRepository.save(challenge);
        log.info("ShuleHub 2FA challenge started userId={} setupRequired={}", user.getId(), !enrolled);
        return LoginResponse.challenge(
                rawToken,
                !enrolled,
                pendingSecret,
                pendingSecret == null ? null : totpService.otpauthUri(user.getEmail(), pendingSecret));
    }

    @Transactional
    public User verify(VerifyTwoFactorRequest request) {
        TwoFactorChallenge challenge = challengeRepository
                .findByTokenHashAndConsumedFalse(TokenHash.sha256(request.pendingToken()))
                .orElseThrow(() -> new BusinessException("Sign in again to continue"));
        if (challenge.getExpiresAt() == null || challenge.getExpiresAt().isBefore(Instant.now())) {
            challenge.setConsumed(true);
            throw new BusinessException("Authenticator check expired. Sign in again.");
        }
        User user = challenge.getUser();
        String secret = challenge.getPendingSecret() != null && !challenge.getPendingSecret().isBlank()
                ? challenge.getPendingSecret()
                : user.getTotpSecret();
        if (!totpService.verify(secret, request.code())) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            if (challenge.getFailedAttempts() >= MAX_ATTEMPTS) {
                challenge.setConsumed(true);
                log.warn("ShuleHub 2FA challenge exhausted userId={}", user.getId());
                throw new BusinessException(INVALID_CODE);
            }
            throw new BusinessException(INVALID_CODE);
        }
        if (challenge.getPendingSecret() != null && !challenge.getPendingSecret().isBlank()) {
            user.setTotpSecret(challenge.getPendingSecret());
            user.setTotpEnabled(true);
        }
        challenge.setConsumed(true);
        challenge.setPendingSecret(null);
        log.info("ShuleHub 2FA verified userId={}", user.getId());
        return user;
    }

    @Transactional
    public void reset(User user) {
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        if (user.getId() != null) {
            challengeRepository.deleteByUserId(user.getId());
        }
        log.info("ShuleHub 2FA enrollment cleared userId={}", user.getId());
    }

    private static SchoolTwoFactorResponse toResponse(School school, String tenantName) {
        return new SchoolTwoFactorResponse(school.getId(), school.getName(), tenantName, school.isTwoFactorEnabled());
    }
}
