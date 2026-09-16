package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "result_weight_configs")
public class ResultWeightConfig extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_term_id")
    private AcademicTerm academicTerm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal midtermWeight = new BigDecimal("10.00");

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal semiExamWeight = new BigDecimal("90.00");

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal semiResultWeight = new BigDecimal("50.00");

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal terminalExamWeight = new BigDecimal("50.00");
}
