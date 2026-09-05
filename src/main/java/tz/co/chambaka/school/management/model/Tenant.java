package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.TenantStatus;
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
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 80)
    private String country;

    @Column(nullable = false, length = 80)
    private String timezone = "Africa/Dar_es_Salaam";

    @Column(nullable = false, length = 10)
    private String currency = "TZS";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantStatus status = TenantStatus.TRIAL;

    @Column(nullable = false, length = 30)
    private String subscriptionPlan = "STARTER";

    private Instant trialEndsAt;
}
