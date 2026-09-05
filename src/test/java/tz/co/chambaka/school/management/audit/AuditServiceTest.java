package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.logging.RequestContext;
import tz.co.chambaka.school.management.model.AuditEvent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AuditEventRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;
    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void context() {
        RequestContext.set("corr-audit-1", "10.0.0.8", "JUnit");
    }

    @AfterEach
    void cleanup() {
        RequestContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsDraftAndFillsActorFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(Fixtures.principal(Role.ADMIN), null));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        auditService.record(new AuditEventDraft()
                .action(AuditAction.CREATE)
                .resourceType("Student")
                .resourceId("9")
                .summary("created")
                .details("{\"password\":\"secret\"}")
                .httpMethod("POST")
                .httpPath("/api/v1/students")
                .statusCode(201));
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getCorrectionId()).isEqualTo("corr-audit-1");
        assertThat(event.getScope()).isEqualTo(AuditScope.TENANT);
        assertThat(event.getActorEmail()).isEqualTo("admin@example.com");
        assertThat(event.getSchoolId()).isEqualTo(1L);
        assertThat(event.getDetails()).doesNotContain("secret");
        assertThat(event.getIpAddress()).isEqualTo("10.0.0.8");
    }

    @Test
    void ignoresNullDraftAndSwallowsSaveErrors() {
        auditService.record(null);
        auditService.record(new AuditEventDraft());
        verify(auditEventRepository, never()).save(any());
        doThrow(new RuntimeException("db")).when(auditEventRepository).save(any());
        auditService.record(new AuditEventDraft().action(AuditAction.ERROR).scope(AuditScope.PLATFORM));
        verify(auditEventRepository).save(any());
    }

    @Test
    void recordsAuthVariants() {
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        User admin = Fixtures.user(2L, Role.ADMIN);
        auditService.recordAuth(AuditAction.LOGIN, admin, "in");
        User superAdmin = Fixtures.user(1L, Role.SUPER_ADMIN);
        auditService.recordAuth(AuditAction.LOGIN, superAdmin, "platform");
        auditService.recordAuth(AuditAction.REGISTER_SCHOOL, admin, "onboard");
        auditService.recordAuth(AuditAction.LOGIN, null, "anon");
        auditService.recordAuthFailure("bad@example.com", "Invalid email or password");
        verify(auditEventRepository, org.mockito.Mockito.times(5)).save(any());
    }

    @Test
    void generatesCorrectionIdWhenMissing() {
        RequestContext.clear();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        auditService.record(new AuditEventDraft()
                .scope(AuditScope.PLATFORM)
                .action(AuditAction.ACCESS)
                .actorUserId(1L)
                .actorEmail("a@b.com")
                .actorRole("ADMIN")
                .schoolId(4L)
                .httpPath("/x"));
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertThat(captor.getValue().getCorrectionId()).isNotBlank();
        assertThat(captor.getValue().getSchoolId()).isEqualTo(4L);
        UserPrincipal unused = new UserPrincipal(Fixtures.user(2L, Role.TEACHER));
        assertThat(unused.getRole()).isEqualTo(Role.TEACHER);
    }
}
