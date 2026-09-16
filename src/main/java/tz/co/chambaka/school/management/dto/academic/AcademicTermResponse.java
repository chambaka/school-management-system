package tz.co.chambaka.school.management.dto.academic;

import java.time.LocalDate;

public record AcademicTermResponse(
        Long id,
        Long academicYearId,
        String academicYearName,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentTerm
) {
}
