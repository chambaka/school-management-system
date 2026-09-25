package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.Gender;

import java.time.LocalDate;

public record UpdateStudentRequest(
        String name,
        String firstName,
        String middleName,
        String lastName,
        String nationality,
        String phone,
        String rollNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodGroup,
        String address,
        String emergencyContact,
        String medicalNotes,
        String insuranceProvider,
        String insuranceMembershipNo,
        LocalDate insuranceExpiry,
        Long academicYearId,
        Long schoolClassId,
        Long sectionId,
        Boolean enabled
) {
    public UpdateStudentRequest(
            String name, String phone, String rollNumber, LocalDate dateOfBirth, Gender gender,
            String bloodGroup, String address, String emergencyContact, String medicalNotes,
            Long academicYearId, Long schoolClassId, Long sectionId, Boolean enabled
    ) {
        this(name, null, null, null, null, phone, rollNumber, dateOfBirth, gender, bloodGroup, address,
                emergencyContact, medicalNotes, null, null, null, academicYearId, schoolClassId, sectionId, enabled);
    }
}
