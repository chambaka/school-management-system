package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.support.Fixtures;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ContentCachingRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HttpAuditFilterTest {

    @Mock
    private AuditService auditService;
    @InjectMocks
    private HttpAuditFilter filter;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsMutatingRequestWithBody() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(Fixtures.principal(Role.ADMIN), null));
        MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/v1/students");
        raw.setQueryString("dryRun=false");
        raw.setContent("{\"password\":\"secret\",\"name\":\"Ada\"}".getBytes());
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(raw);
        request.getInputStream().readAllBytes();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        filter.doFilter(request, response, mockChain());
        ArgumentCaptor<AuditEventDraft> captor = ArgumentCaptor.forClass(AuditEventDraft.class);
        verify(auditService).record(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(captor.getValue().getResourceType()).isEqualTo("Student");
        assertThat(captor.getValue().getActorEmail()).isEqualTo("admin@example.com");
        assertThat(captor.getValue().getDetails()).doesNotContain("secret");
    }

    @Test
    void skipsSuccessfulReadsAndSwallowsAuditErrors() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        filter.doFilter(request, response, mockChain());
        verify(auditService, never()).record(any());

        MockHttpServletRequest financeRead = new MockHttpServletRequest("GET", "/api/v1/invoices/4");
        MockHttpServletResponse financeOk = new MockHttpServletResponse();
        financeOk.setStatus(200);
        filter.doFilter(financeRead, financeOk, mockChain());
        verify(auditService, times(1)).record(any());

        MockHttpServletRequest denied = new MockHttpServletRequest("GET", "/api/v1/invoices/4");
        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        forbidden.setStatus(403);
        doThrow(new RuntimeException("audit down")).when(auditService).record(any());
        filter.doFilter(denied, forbidden, mockChain());
        verify(auditService, times(2)).record(any());
    }

    @Test
    void recordsAnonymousFailureWithoutCachedBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/schools/current");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);
        filter.doFilter(request, response, mockChain());
        ArgumentCaptor<AuditEventDraft> captor = ArgumentCaptor.forClass(AuditEventDraft.class);
        verify(auditService).record(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(captor.getValue().getActorEmail()).isNull();
        assertThat(captor.getValue().getDetails()).isNull();
    }

    private static FilterChain mockChain() {
        return (req, res) -> {
        };
    }
}
