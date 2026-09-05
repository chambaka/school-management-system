package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "academic_years")
public class AcademicYear extends TenantEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(name = "current_year", nullable = false)
    private boolean currentYear = false;
}
