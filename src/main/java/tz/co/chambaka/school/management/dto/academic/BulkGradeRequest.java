package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BulkGradeRequest(
        @NotNull Long examId,
        @NotNull Long subjectId,
        @NotEmpty @Valid List<Entry> entries
) {
    public record Entry(
            @NotNull Long studentId,
            @NotNull BigDecimal marksObtained,
            String remarks
    ) {
    }
}
