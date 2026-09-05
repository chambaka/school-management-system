package tz.co.chambaka.school.management.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();

    @Test
    void logsApiRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/students");
        request.setQueryString("page=0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsDocsAndActuator() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        for (String path : new String[]{"/actuator/health", "/swagger-ui/index.html", "/v3/api-docs"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            filter.doFilter(request, response, chain);
        }
        verify(chain, org.mockito.Mockito.times(3)).doFilter(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
