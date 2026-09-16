package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LessonLogRequest(
        Long timetableSlotId,
        @NotNull Long sectionId,
        @NotNull Long subjectId,
        @NotNull LocalDate lessonDate,
        @NotBlank String topic,
        String objectives,
        String materials,
        String homework
) {
}
