package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subjects")
public class Subject extends TenantEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(length = 500)
    private String description;
}
