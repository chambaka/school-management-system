package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.PeriodKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "bell_periods")
public class BellPeriod extends TenantEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodKind kind = PeriodKind.LESSON;

    @Column(nullable = false)
    private int sortOrder = 0;
}
