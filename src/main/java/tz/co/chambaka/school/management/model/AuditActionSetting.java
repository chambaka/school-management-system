package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_action_settings")
public class AuditActionSetting {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_action", length = 40, nullable = false)
    private AuditAction action;

    @Column(nullable = false)
    private boolean enabled = true;
}
