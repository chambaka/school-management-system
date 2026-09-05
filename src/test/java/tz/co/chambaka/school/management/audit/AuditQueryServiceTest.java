package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AuditEvent;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;
    @InjectMocks
    private AuditQueryService service;

    @Test
    void searchMapsPage() {
        AuditEvent event = sample();
        when(auditEventRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(event)));
        var page = service.search(AuditScope.TENANT, 1L, "cid", AuditAction.CREATE, "Student",
                "a@b.com", Instant.now().minusSeconds(60), Instant.now(), PageRequest.of(0, 10));
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().correctionId()).isEqualTo("cid-1");
        assertThat(page.content().getFirst().action()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void byCorrectionIdPlatformAndTenant() {
        when(auditEventRepository.findByCorrectionIdOrderByCreatedAtAsc("cid-1"))
                .thenReturn(List.of(sample()));
        assertThat(service.byCorrectionId("cid-1")).hasSize(1);
        when(auditEventRepository.findByCorrectionIdAndSchoolIdOrderByCreatedAtAsc("cid-1", 1L))
                .thenReturn(List.of(sample()));
        assertThat(service.byCorrectionIdForSchool("cid-1", 1L)).hasSize(1);
        when(auditEventRepository.findByCorrectionIdOrderByCreatedAtAsc("missing")).thenReturn(List.of());
        assertThatThrownBy(() -> service.byCorrectionId("missing")).isInstanceOf(ResourceNotFoundException.class);
        when(auditEventRepository.findByCorrectionIdAndSchoolIdOrderByCreatedAtAsc("missing", 1L))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.byCorrectionIdForSchool("missing", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static AuditEvent sample() {
        AuditEvent event = new AuditEvent();
        event.setId(8L);
        event.setCorrectionId("cid-1");
        event.setScope(AuditScope.TENANT);
        event.setSchoolId(1L);
        event.setActorUserId(2L);
        event.setActorEmail("a@b.com");
        event.setActorRole("ADMIN");
        event.setAction(AuditAction.CREATE);
        event.setResourceType("Student");
        event.setResourceId("9");
        event.setSummary("ok");
        event.setDetails("{}");
        event.setHttpMethod("POST");
        event.setHttpPath("/api/v1/students");
        event.setStatusCode(201);
        event.setIpAddress("127.0.0.1");
        event.setUserAgent("JUnit");
        event.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return event;
    }
}
