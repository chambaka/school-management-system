package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.Gender;

import java.time.LocalDate;

public record StudentResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String admissionNo,
        String rollNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodGroup,
        LocalDate admissionDate,
        String address,
        String emergencyContact,
        Long academicYearId,
        Long schoolClassId,
        String schoolClassName,
        Long sectionId,
        String sectionName,
        boolean enabled
) {
}
