package tz.co.chambaka.school.management.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correctionId,
        List<FieldErrorDetail> errors
) {
    public record FieldErrorDetail(String field, String message) {
    }
}
