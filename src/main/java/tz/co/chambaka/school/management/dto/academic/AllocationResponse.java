package tz.co.chambaka.school.management.dto.academic;

public record AllocationResponse(
        Long id,
        Long teacherId,
        String teacherName,
        Long subjectId,
        String subjectName,
        Long schoolClassId,
        String schoolClassName,
        Long sectionId,
        String sectionName,
        Long academicYearId
) {
}
