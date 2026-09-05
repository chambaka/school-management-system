package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.AuditEvent;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditEventSpecsTest {

    @Test
    void buildsEmptyAndFullPredicates() {
        Root<AuditEvent> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get(any(String.class))).thenReturn(path);
        when(cb.conjunction()).thenReturn(predicate);
        when(cb.and(any(), any())).thenReturn(predicate);
        when(cb.equal(any(), any())).thenReturn(predicate);
        when(cb.lower(any(Expression.class))).thenReturn(path);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(Instant.class))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), any(Instant.class))).thenReturn(predicate);

        Specification<AuditEvent> empty = AuditEventSpecs.matching(null, null, " ", null, " ", " ", null, null);
        assertThat(empty.toPredicate(root, query, cb)).isEqualTo(predicate);

        Specification<AuditEvent> full = AuditEventSpecs.matching(
                AuditScope.TENANT, 1L, "cid", AuditAction.CREATE, "Student",
                "A@B.com", Instant.now().minusSeconds(10), Instant.now());
        assertThat(full.toPredicate(root, query, cb)).isEqualTo(predicate);
    }
}
