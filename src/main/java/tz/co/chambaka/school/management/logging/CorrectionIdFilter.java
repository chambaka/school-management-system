package tz.co.chambaka.school.management.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrectionIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String correctionId = CorrectionIds.resolve(
                CorrectionIds.normalize(request.getHeader(CorrectionIds.HEADER)),
                CorrectionIds.normalize(request.getHeader(CorrectionIds.ALIAS_HEADER))
        );
        String ip = CorrectionIds.clientIp(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
        String userAgent = request.getHeader("User-Agent");
        RequestContext.set(correctionId, ip, userAgent);
        RequestMdc.putCorrectionId(correctionId);
        response.setHeader(CorrectionIds.HEADER, correctionId);
        HttpServletRequest wrapped = request instanceof ContentCachingRequestWrapper
                ? request
                : new ContentCachingRequestWrapper(request, 4096);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            RequestContext.clear();
            RequestMdc.clear();
        }
    }
}
