package tz.co.chambaka.school.management.dto.student;

import tz.co.chambaka.school.management.model.enums.RelationshipType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AdmitParentRequest(
        String name,
        @Email String email,
        @Size(min = 8, max = 72) String password,
        String phone,
        String occupation,
        String address,
        RelationshipType relationship
) {
    public boolean provided() {
        return notBlank(name) || notBlank(email) || notBlank(phone);
    }

    public boolean complete() {
        return notBlank(name) && notBlank(email);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
