package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_parents")
public class StudentParent extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RelationshipType relationship;

    @Column(nullable = false)
    private boolean primaryContact = false;
}
