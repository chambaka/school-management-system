package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.dto.audit.AuditEventResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AuditEvent;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.repository.AuditEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    public AuditQueryService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> search(
            AuditScope scope,
            Long schoolId,
            String correctionId,
            AuditAction action,
            String resourceType,
            String actorEmail,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        var spec = AuditEventSpecs.matching(scope, schoolId, correctionId, action, resourceType, actorEmail, from, to);
        return PageResponse.of(auditEventRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> byCorrectionId(String correctionId) {
        List<AuditEvent> events = auditEventRepository.findByCorrectionIdOrderByCreatedAtAsc(correctionId);
        if (events.isEmpty()) {
            throw new ResourceNotFoundException("No audit events for correctionId: " + correctionId);
        }
        return events.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> byCorrectionIdForSchool(String correctionId, Long schoolId) {
        List<AuditEvent> events = auditEventRepository.findByCorrectionIdAndSchoolIdOrderByCreatedAtAsc(
                correctionId, schoolId);
        if (events.isEmpty()) {
            throw new ResourceNotFoundException("No audit events for correctionId: " + correctionId);
        }
        return events.stream().map(this::toResponse).toList();
    }

    AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getCorrectionId(),
                event.getScope(),
                event.getSchoolId(),
                event.getActorUserId(),
                event.getActorEmail(),
                event.getActorRole(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getSummary(),
                event.getDetails(),
                event.getHttpMethod(),
                event.getHttpPath(),
                event.getStatusCode(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getCreatedAt()
        );
    }
}
