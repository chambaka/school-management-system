package tz.co.chambaka.school.management.dto.finance;

import tz.co.chambaka.school.management.model.enums.FeeFrequency;
import tz.co.chambaka.school.management.model.enums.FeeType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeStructureResponse(
        Long id,
        Long academicYearId,
        Long schoolClassId,
        String schoolClassName,
        String name,
        FeeType feeType,
        FeeFrequency frequency,
        BigDecimal amount,
        LocalDate dueDate
) {
}
