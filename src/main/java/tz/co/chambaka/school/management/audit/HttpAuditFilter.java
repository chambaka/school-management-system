package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpAuditFilter.class);

    private final AuditService auditService;

    public HttpAuditFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            recordIfNeeded(request, response);
        }
    }

    private void recordIfNeeded(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();
        if (!AuditPathClassifier.shouldRecord(method, path, status)) {
            return;
        }
        try {
            UserPrincipal principal = currentPrincipal();
            Role role = principal == null ? null : principal.getRole();
            AuditAction action = AuditPathClassifier.actionFromHttp(method, status);
            String resourceType = AuditPathClassifier.resourceType(path);
            String resourceId = AuditPathClassifier.resourceId(path);
            String query = request.getQueryString();
            String details = SensitiveDataSanitizer.sanitize(body(request));
            if (query != null && !query.isBlank()) {
                details = SensitiveDataSanitizer.sanitize("query=" + query + (details == null ? "" : " body=" + details));
            }
            auditService.record(new AuditEventDraft()
                    .scope(AuditPathClassifier.scope(path, role, action))
                    .action(action)
                    .schoolId(principal == null ? null : principal.getSchoolId())
                    .actorUserId(principal == null ? null : principal.getId())
                    .actorEmail(principal == null ? null : principal.getEmail())
                    .actorRole(role == null ? null : role.name())
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .summary(method + " " + path + " -> " + status)
                    .details(details)
                    .httpMethod(method)
                    .httpPath(path)
                    .statusCode(status));
        } catch (RuntimeException ex) {
            log.error("Failed to write HTTP audit event for {} {}", method, path, ex);
        }
    }

    private static String body(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length == 0) {
                return null;
            }
            return new String(buf, StandardCharsets.UTF_8);
        }
        return null;
    }

    private static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
