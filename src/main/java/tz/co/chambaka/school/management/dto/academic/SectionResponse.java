package tz.co.chambaka.school.management.dto.academic;

public record SectionResponse(
        Long id,
        Long schoolClassId,
        String schoolClassName,
        String name,
        Integer capacity,
        Long classTeacherId,
        String classTeacherName
) {
}
