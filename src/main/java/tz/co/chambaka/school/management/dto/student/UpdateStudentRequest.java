package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.Gender;

import java.time.LocalDate;

public record UpdateStudentRequest(
        String name,
        String phone,
        String rollNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodGroup,
        String address,
        String emergencyContact,
        Long academicYearId,
        Long schoolClassId,
        Long sectionId,
        Boolean enabled
) {
}
