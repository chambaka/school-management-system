package tz.co.chambaka.school.management.curriculum;

import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.model.TenantEntity;
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
@Table(name = "curriculum_topics")
public class CurriculumTopic extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 1000)
    private String objectives;

    @Column(nullable = false)
    private int sortOrder = 0;
}
