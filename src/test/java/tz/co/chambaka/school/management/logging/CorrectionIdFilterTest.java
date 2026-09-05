package tz.co.chambaka.school.management.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrectionIdFilterTest {

    private final CorrectionIdFilter filter = new CorrectionIdFilter();

    @AfterEach
    void cleanup() {
        RequestContext.clear();
        RequestMdc.clear();
    }

    @Test
    void generatesAndEchoesCorrectionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("X-Forwarded-For", "10.1.1.1");
        request.addHeader("User-Agent", "test-agent");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertThat(RequestContext.getCorrectionId()).isNotBlank();
            assertThat(RequestContext.getIpAddress()).isEqualTo("10.1.1.1");
            assertThat(req).isInstanceOf(ContentCachingRequestWrapper.class);
        };
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader(CorrectionIds.HEADER)).isNotBlank();
        assertThat(RequestContext.getCorrectionId()).isNull();
    }

    @Test
    void reusesIncomingAndAlreadyWrappedRequest() throws Exception {
        MockHttpServletRequest raw = new MockHttpServletRequest("POST", "/api/v1/students");
        raw.addHeader(CorrectionIds.HEADER, "corr-incoming-1");
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(raw);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(req).isSameAs(wrapped);
        filter.doFilter(wrapped, response, chain);
        assertThat(response.getHeader(CorrectionIds.HEADER)).isEqualTo("corr-incoming-1");
    }

    @Test
    void usesAliasHeaderWhenPrimaryMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(CorrectionIds.ALIAS_HEADER, "alias-corr-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        assertThat(response.getHeader(CorrectionIds.HEADER)).isEqualTo("alias-corr-1");
    }

    @Test
    void stillClearsWhenChainThrows() throws Exception {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(chain).doFilter(any(), any());
        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException ignored) {
            // expected
        }
        assertThat(RequestContext.getCorrectionId()).isNull();
        verify(chain).doFilter(any(), any());
    }
}
