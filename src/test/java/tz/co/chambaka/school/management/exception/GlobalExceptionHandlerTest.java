package tz.co.chambaka.school.management.exception;

import tz.co.chambaka.school.management.logging.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = request("/api/v1/x");

    @Test
    void apiException() {
        RequestContext.set("corr-err-1", "127.0.0.1", "JUnit");
        try {
            var response = handler.handleApi(new ResourceNotFoundException("missing"), request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().message()).isEqualTo("missing");
            assertThat(response.getBody().correctionId()).isEqualTo("corr-err-1");
        } finally {
            RequestContext.clear();
        }
    }

    @Test
    void validation() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "email", "must not be blank"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);
        var response = handler.handleValidation(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).hasSize(1);
        assertThat(response.getBody().errors().getFirst().field()).isEqualTo("email");
    }

    @Test
    void constraint() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("invalid");
        var response = handler.handleConstraint(new ConstraintViolationException(Set.of(violation)), request);
        assertThat(response.getBody().errors().getFirst().field()).isEqualTo("name");
    }

    @Test
    void credentialsAndAuthAndDeniedAndGeneric() {
        assertThat(handler.handleBadCredentials(new BadCredentialsException("bad"), request).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleAuth(new InsufficientAuthenticationException("x"), request).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleDenied(new AccessDeniedException("no"), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleGeneric(new RuntimeException("boom"), request).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void resourceFactoryAndDuplicates() {
        assertThat(ResourceNotFoundException.of("User", 3L).getMessage()).contains("User not found: 3");
        assertThat(new DuplicateResourceException("dup").getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(new BusinessException("biz").getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @SuppressWarnings("unused")
    private void sample(String email) {
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }
}
