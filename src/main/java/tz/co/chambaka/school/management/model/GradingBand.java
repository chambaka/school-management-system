package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "grading_bands")
public class GradingBand extends TenantEntity {

    @Column(nullable = false)
    private int minPercent;

    @Column(nullable = false)
    private int maxPercent;

    @Column(nullable = false, length = 8)
    private String letter;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;

    @Column(nullable = false)
    private int sortOrder = 0;
}
