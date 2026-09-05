package tz.co.chambaka.school.management.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTeacherRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        String phone,
        @NotBlank String employeeId,
        String qualification,
        String specialization,
        String department,
        LocalDate joiningDate
) {
}
