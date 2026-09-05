package tz.co.chambaka.school.management.dto.academic;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableRequest(
        @NotNull Long academicYearId,
        @NotNull Long sectionId,
        @NotNull Long subjectId,
        @NotNull Long teacherId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String room
) {
}
