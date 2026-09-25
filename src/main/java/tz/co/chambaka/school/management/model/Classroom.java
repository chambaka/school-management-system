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
@Table(name = "classrooms")
public class Classroom extends TenantEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String code;

    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building site;

    @Column(length = 100)
    private String building;

    @Column(length = 500)
    private String notes;
}
