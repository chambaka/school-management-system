package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.ParentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parents")
public class Parent extends TenantEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(length = 120)
    private String occupation;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ParentStatus status = ParentStatus.ACTIVE;
}
