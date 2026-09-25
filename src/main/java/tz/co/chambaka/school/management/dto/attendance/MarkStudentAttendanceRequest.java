package tz.co.chambaka.school.management.dto.attendance;

import tz.co.chambaka.school.management.model.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MarkStudentAttendanceRequest(
        @NotNull Long sectionId,
        @NotNull LocalDate date,
        @NotEmpty @Valid List<StudentAttendanceItem> entries,
        Long timetableSlotId
) {
    public MarkStudentAttendanceRequest(Long sectionId, LocalDate date, List<StudentAttendanceItem> entries) {
        this(sectionId, date, entries, null);
    }
    public record StudentAttendanceItem(
            @NotNull Long studentId,
            @NotNull AttendanceStatus status,
            String remarks
    ) {
    }
}
