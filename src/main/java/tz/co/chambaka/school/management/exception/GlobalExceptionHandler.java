package tz.co.chambaka.school.management.exception;

import tz.co.chambaka.school.management.logging.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        log.warn("API error status={} path={} message={}", ex.getStatus().value(), request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toField)
                .toList();
        log.warn("Validation failed path={} fields={}", request.getRequestURI(), fields.size());
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fields = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.warn("Constraint violation path={} fields={}", request.getRequestURI(), fields.size());
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), fields);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials path={}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request.getRequestURI(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication required path={}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied path={}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error path={}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI(), null);
    }

    private ErrorResponse.FieldErrorDetail toField(FieldError error) {
        return new ErrorResponse.FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path,
                                                List<ErrorResponse.FieldErrorDetail> errors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                RequestContext.getCorrectionId(),
                errors
        );
        return ResponseEntity.status(status).body(body);
    }
}
