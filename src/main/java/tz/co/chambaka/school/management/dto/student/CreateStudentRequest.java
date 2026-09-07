package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateStudentRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        String phone,
        String rollNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodGroup,
        LocalDate admissionDate,
        String address,
        String emergencyContact,
        Long academicYearId,
        Long schoolClassId,
        Long sectionId
) {
}
