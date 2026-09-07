package tz.co.chambaka.school.management.dto.teacher;

import tz.co.chambaka.school.management.model.enums.TeacherStatus;

import java.time.LocalDate;

public record TeacherResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String employeeId,
        String qualification,
        String specialization,
        String department,
        LocalDate joiningDate,
        boolean enabled,
        TeacherStatus status,
        String photoUrl
) {
}
