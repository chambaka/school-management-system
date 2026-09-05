package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "school_classes")
public class SchoolClass extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false)
    private int displayOrder = 0;
}
