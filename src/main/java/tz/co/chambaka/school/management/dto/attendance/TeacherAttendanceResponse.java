package tz.co.chambaka.school.management.dto.attendance;

import tz.co.chambaka.school.management.model.enums.AttendanceStatus;

import java.time.LocalDate;

public record TeacherAttendanceResponse(
        Long id,
        Long teacherId,
        String teacherName,
        LocalDate date,
        AttendanceStatus status,
        String remarks
) {
}
