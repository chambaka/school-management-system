package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "teachers")
public class Teacher extends TenantEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String employeeId;

    @Column(length = 150)
    private String qualification;

    @Column(length = 150)
    private String specialization;

    @Column(length = 100)
    private String department;

    private LocalDate joiningDate;
}
