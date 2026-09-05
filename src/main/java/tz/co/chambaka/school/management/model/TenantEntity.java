package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;
}
