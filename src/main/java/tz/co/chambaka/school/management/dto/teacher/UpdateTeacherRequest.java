package tz.co.chambaka.school.management.dto.teacher;

import java.time.LocalDate;

public record UpdateTeacherRequest(
        String name,
        String phone,
        String qualification,
        String specialization,
        String department,
        LocalDate joiningDate,
        Boolean enabled,
        String employeeId
) {
    public UpdateTeacherRequest(
            String name,
            String phone,
            String qualification,
            String specialization,
            String department,
            LocalDate joiningDate,
            Boolean enabled
    ) {
        this(name, phone, qualification, specialization, department, joiningDate, enabled, null);
    }
}
