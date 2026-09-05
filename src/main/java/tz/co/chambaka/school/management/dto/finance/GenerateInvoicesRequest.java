package tz.co.chambaka.school.management.dto.finance;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record GenerateInvoicesRequest(
        @NotNull Long academicYearId,
        @NotNull Long schoolClassId,
        @NotEmpty List<Long> feeStructureIds,
        LocalDate dueDate
) {
}
