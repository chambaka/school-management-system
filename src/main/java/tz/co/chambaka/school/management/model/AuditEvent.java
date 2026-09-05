package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_events")
public class AuditEvent extends BaseEntity {

    @Column(name = "correction_id", nullable = false, length = 64)
    private String correctionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditScope scope;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 180)
    private String actorEmail;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "resource_type", length = 80)
    private String resourceType;

    @Column(name = "resource_id", length = 80)
    private String resourceId;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "http_path", length = 500)
    private String httpPath;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
