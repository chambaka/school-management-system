package tz.co.chambaka.school.management.dto.academic;

import tz.co.chambaka.school.management.model.enums.ExamType;

import java.time.LocalDate;

public record ExamResponse(
        Long id,
        Long academicYearId,
        Long schoolClassId,
        String schoolClassName,
        String name,
        ExamType examType,
        LocalDate startDate,
        LocalDate endDate,
        boolean published
) {
}
