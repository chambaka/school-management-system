package tz.co.chambaka.school.management.security;

import tz.co.chambaka.school.management.logging.RequestContext;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.support.Fixtures;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void continuesWithoutHeader() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesBearerToken() throws Exception {
        Claims claims = mockClaims("admin@example.com");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer tok");
        when(jwtService.parse("tok")).thenReturn(claims);
        when(userDetailsService.loadUserByUsername("admin@example.com"))
                .thenReturn(new UserPrincipal(Fixtures.user(2L, Role.ADMIN)));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void writesUnauthorizedOnBadToken() throws Exception {
        RequestContext.set("corr-jwt-1", "127.0.0.1", "JUnit");
        try {
            when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad");
            when(jwtService.parse("bad")).thenThrow(new JwtException("expired"));
            StringWriter body = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(body));
            filter.doFilter(request, response, chain);
            verify(chain, never()).doFilter(request, response);
            verify(response).setStatus(401);
            assertThat(body.toString()).contains("Invalid or expired token");
            assertThat(body.toString()).contains("corr-jwt-1");
        } finally {
            RequestContext.clear();
        }
    }

    @Test
    void unauthorizedWithoutCorrectionIdOmitsField() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad");
        when(jwtService.parse("bad")).thenThrow(new JwtException("expired"));
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        filter.doFilter(request, response, chain);
        assertThat(body.toString()).contains("Invalid or expired token");
        assertThat(body.toString()).doesNotContain("correctionId");
    }

    @Test
    void skipsWhenEmailMissingOrDisabled() throws Exception {
        Claims claims = mockClaims(null);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer tok");
        when(jwtService.parse("tok")).thenReturn(claims);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        Claims enabledClaims = mockClaims("admin@example.com");
        when(jwtService.parse("tok")).thenReturn(enabledClaims);
        User disabled = Fixtures.user(2L, Role.ADMIN);
        disabled.setEnabled(false);
        when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(new UserPrincipal(disabled));
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresNonBearer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc");
        filter.doFilter(request, response, chain);
        verify(jwtService, never()).parse(anyString());
        verify(chain).doFilter(request, response);
    }

    private Claims mockClaims(String email) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        org.mockito.Mockito.lenient().when(claims.get("email", String.class)).thenReturn(email);
        return claims;
    }
}
