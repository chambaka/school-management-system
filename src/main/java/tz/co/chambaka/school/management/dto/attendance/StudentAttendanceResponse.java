package tz.co.chambaka.school.management.dto.attendance;

import tz.co.chambaka.school.management.model.enums.AttendanceStatus;

import java.time.LocalDate;

public record StudentAttendanceResponse(
        Long id,
        Long studentId,
        String studentName,
        Long sectionId,
        LocalDate date,
        AttendanceStatus status,
        String remarks,
        Long timetableSlotId
) {
    public StudentAttendanceResponse(
            Long id, Long studentId, String studentName, Long sectionId, LocalDate date,
            AttendanceStatus status, String remarks
    ) {
        this(id, studentId, studentName, sectionId, date, status, remarks, null);
    }
}
