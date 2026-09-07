package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.SmsDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sms_deliveries")
public class SmsDelivery extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "communication_id")
    private StudentCommunication communication;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsDeliveryStatus status = SmsDeliveryStatus.SKIPPED;

    @Column(length = 80)
    private String providerRef;

    @Column(length = 250)
    private String error;

    @Column(length = 320)
    private String body;
}
