package tz.co.chambaka.school.management.dto.academic;

public record SchoolClassResponse(
        Long id,
        Long academicYearId,
        String academicYearName,
        String name,
        String code,
        int displayOrder
) {
}
