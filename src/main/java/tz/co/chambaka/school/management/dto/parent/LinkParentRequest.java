package tz.co.chambaka.school.management.dto.parent;

import tz.co.chambaka.school.management.model.enums.RelationshipType;
import jakarta.validation.constraints.NotNull;

public record LinkParentRequest(
        @NotNull Long parentId,
        @NotNull RelationshipType relationship,
        boolean primaryContact,
        boolean emergencyContact,
        boolean invoiceRecipient
) {
    public LinkParentRequest(Long parentId, RelationshipType relationship, boolean primaryContact) {
        this(parentId, relationship, primaryContact, false, false);
    }
}
