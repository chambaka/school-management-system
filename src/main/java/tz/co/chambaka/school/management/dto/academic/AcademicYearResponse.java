package tz.co.chambaka.school.management.dto.academic;

import java.time.LocalDate;

public record AcademicYearResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentYear
) {
}
