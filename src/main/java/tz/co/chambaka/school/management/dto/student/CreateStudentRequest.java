package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateStudentRequest(
        @NotBlank String name,
        String firstName,
        String middleName,
        String lastName,
        String nationality,
        @NotBlank @Email String email,
        @Size(min = 8, max = 72) String password,
        String phone,
        String rollNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String bloodGroup,
        LocalDate admissionDate,
        String address,
        String emergencyContact,
        String medicalNotes,
        Long academicYearId,
        Long schoolClassId,
        Long sectionId,
        AdmitParentRequest parent
) {
    public CreateStudentRequest(
            String name, String firstName, String middleName, String lastName, String nationality,
            String email, String password, String phone, String rollNumber,
            LocalDate dateOfBirth, Gender gender, String bloodGroup, LocalDate admissionDate,
            String address, String emergencyContact, String medicalNotes,
            Long academicYearId, Long schoolClassId, Long sectionId
    ) {
        this(name, firstName, middleName, lastName, nationality, email, password, phone, rollNumber, dateOfBirth,
                gender, bloodGroup, admissionDate, address, emergencyContact, medicalNotes,
                academicYearId, schoolClassId, sectionId, null);
    }

    public CreateStudentRequest(
            String name, String email, String password, String phone, String rollNumber,
            LocalDate dateOfBirth, Gender gender, String bloodGroup, LocalDate admissionDate,
            String address, String emergencyContact, String medicalNotes,
            Long academicYearId, Long schoolClassId, Long sectionId
    ) {
        this(name, null, null, null, null, email, password, phone, rollNumber, dateOfBirth, gender, bloodGroup,
                admissionDate, address, emergencyContact, medicalNotes, academicYearId, schoolClassId, sectionId, null);
    }
}
