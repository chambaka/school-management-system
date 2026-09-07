package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.model.enums.Gender;
import tz.co.chambaka.school.management.model.enums.StudentStatus;

import java.time.LocalDate;
import java.util.List;

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
        boolean enabled,
        StudentStatus status,
        String photoUrl,
        List<StudentParentResponse> parents
) {
}
