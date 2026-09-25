package tz.co.chambaka.school.management.curriculum;

public record CurriculumTopicResponse(
        Long id,
        Long subjectId,
        String subjectName,
        Long schoolClassId,
        String schoolClassName,
        String title,
        String objectives,
        int sortOrder
) {
}
