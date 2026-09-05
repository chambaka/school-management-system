package tz.co.chambaka.school.management.dto.attendance;

import tz.co.chambaka.school.management.model.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MarkTeacherAttendanceRequest(
        @NotNull LocalDate date,
        @NotEmpty @Valid List<TeacherAttendanceItem> entries
) {
    public record TeacherAttendanceItem(
            @NotNull Long teacherId,
            @NotNull AttendanceStatus status,
            String remarks
    ) {
    }
}
