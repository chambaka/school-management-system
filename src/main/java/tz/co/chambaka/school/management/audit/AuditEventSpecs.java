package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.AuditEvent;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditEventSpecs {

    private AuditEventSpecs() {
    }

    public static Specification<AuditEvent> matching(
            AuditScope scope,
            Long schoolId,
            String correctionId,
            AuditAction action,
            String resourceType,
            String actorEmail,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (scope != null) {
                predicate = cb.and(predicate, cb.equal(root.get("scope"), scope));
            }
            if (schoolId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("schoolId"), schoolId));
            }
            if (correctionId != null && !correctionId.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("correctionId"), correctionId));
            }
            if (action != null) {
                predicate = cb.and(predicate, cb.equal(root.get("action"), action));
            }
            if (resourceType != null && !resourceType.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("resourceType"), resourceType));
            }
            if (actorEmail != null && !actorEmail.isBlank()) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("actorEmail")), actorEmail.toLowerCase()));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicate;
        };
    }
}
