package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "schools")
public class School extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 255)
    private String website;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String faviconUrl;

    @Column(length = 20)
    private String primaryColor = "#1B4B8A";

    @Column(length = 20)
    private String secondaryColor = "#F4B400";

    @Column(length = 20)
    private String accentColor = "#0F9D58";

    @Column(unique = true, length = 255)
    private String customDomain;

    @Column(nullable = false, length = 80)
    private String timezone = "Africa/Dar_es_Salaam";

    @Column(nullable = false, length = 20)
    private String locale = "en";

    @Column(nullable = false, length = 10)
    private String currency = "TZS";

    @Column(length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SchoolStatus status = SchoolStatus.TRIAL;

    @Column(nullable = false, length = 30)
    private String subscriptionPlan = "STARTER";

    private Instant trialEndsAt;

    @Column(nullable = false)
    private boolean financeEnabled = true;

    @Column(nullable = false)
    private boolean attendanceEnabled = true;

    @Column(nullable = false)
    private boolean examsEnabled = true;
}
