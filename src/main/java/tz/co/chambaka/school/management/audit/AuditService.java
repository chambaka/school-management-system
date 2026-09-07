package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.logging.RequestContext;
import tz.co.chambaka.school.management.model.AuditEvent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AuditEventRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;
    private final AuditActionSettingsService settingsService;

    public AuditService(AuditEventRepository auditEventRepository, AuditActionSettingsService settingsService) {
        this.auditEventRepository = auditEventRepository;
        this.settingsService = settingsService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEventDraft draft) {
        if (draft == null || draft.getAction() == null) {
            return;
        }
        if (!settingsService.isEnabled(draft.getAction())) {
            log.debug("Skipping disabled audit action={}", draft.getAction());
            return;
        }
        try {
            AuditEvent event = toEvent(draft);
            auditEventRepository.save(event);
            log.info("audit scope={} action={} resource={}:{} schoolId={} actor={} correctionId={}",
                    event.getScope(), event.getAction(), event.getResourceType(), event.getResourceId(),
                    event.getSchoolId(), event.getActorEmail(), event.getCorrectionId());
        } catch (RuntimeException ex) {
            log.error("Failed to persist audit event action={} correctionId={}",
                    draft.getAction(), RequestContext.getCorrectionId(), ex);
        }
    }

    public void recordAuth(AuditAction action, User user, String summary) {
        AuditScope scope = user != null && user.getRole() == Role.SUPER_ADMIN
                ? AuditScope.PLATFORM
                : (action == AuditAction.REGISTER_SCHOOL ? AuditScope.PLATFORM : AuditScope.TENANT);
        Long schoolId = user == null ? null : user.getSchoolId();
        record(new AuditEventDraft()
                .scope(scope)
                .action(action)
                .schoolId(schoolId)
                .actorUserId(user == null ? null : user.getId())
                .actorEmail(user == null ? null : user.getEmail())
                .actorRole(user == null || user.getRole() == null ? null : user.getRole().name())
                .resourceType("Auth")
                .resourceId(user == null || user.getId() == null ? null : String.valueOf(user.getId()))
                .summary(summary)
                .httpMethod("POST"));
    }

    public void recordFinance(
            Long schoolId,
            AuditAction action,
            String resourceType,
            String resourceId,
            String summary,
            String details
    ) {
        record(new AuditEventDraft()
                .scope(AuditScope.TENANT)
                .schoolId(schoolId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .summary(summary)
                .details(details)
                .statusCode(200));
    }

    public void recordAuthFailure(String email, String summary) {
        record(new AuditEventDraft()
                .scope(AuditScope.PLATFORM)
                .action(AuditAction.LOGIN_FAILED)
                .actorEmail(email)
                .resourceType("Auth")
                .summary(summary)
                .httpMethod("POST")
                .httpPath("/api/v1/auth/login")
                .statusCode(401));
    }

    private AuditEvent toEvent(AuditEventDraft draft) {
        UserPrincipal principal = currentPrincipal();
        AuditEvent event = new AuditEvent();
        event.setCorrectionId(RequestContext.requireCorrectionId());
        event.setScope(draft.getScope() != null ? draft.getScope() : defaultScope(principal, draft.getAction()));
        event.setSchoolId(draft.getSchoolId() != null
                ? draft.getSchoolId()
                : principal == null ? null : principal.getSchoolId());
        event.setActorUserId(draft.getActorUserId() != null
                ? draft.getActorUserId()
                : principal == null ? null : principal.getId());
        event.setActorEmail(draft.getActorEmail() != null
                ? draft.getActorEmail()
                : principal == null ? null : principal.getEmail());
        event.setActorRole(draft.getActorRole() != null
                ? draft.getActorRole()
                : principal == null || principal.getRole() == null ? null : principal.getRole().name());
        event.setAction(draft.getAction());
        event.setResourceType(draft.getResourceType());
        event.setResourceId(draft.getResourceId());
        event.setSummary(SensitiveDataSanitizer.truncate(draft.getSummary(), 500));
        event.setDetails(SensitiveDataSanitizer.sanitize(draft.getDetails()));
        event.setHttpMethod(draft.getHttpMethod());
        event.setHttpPath(SensitiveDataSanitizer.truncate(draft.getHttpPath(), 500));
        event.setStatusCode(draft.getStatusCode());
        event.setIpAddress(RequestContext.getIpAddress());
        event.setUserAgent(SensitiveDataSanitizer.truncate(RequestContext.getUserAgent(), 500));
        return event;
    }

    private static AuditScope defaultScope(UserPrincipal principal, AuditAction action) {
        Role role = principal == null ? null : principal.getRole();
        return AuditPathClassifier.scope(null, role, action);
    }

    private static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
