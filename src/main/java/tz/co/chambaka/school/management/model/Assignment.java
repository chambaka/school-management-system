package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import tz.co.chambaka.school.management.model.enums.AssignmentStatus;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "assignments")
public class Assignment extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String instructions;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(length = 300)
    private String attachmentName;

    @Column(length = 400)
    private String attachmentPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PUBLISHED'")
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    @Column(nullable = false)
    private Instant publishedAt = Instant.now();

    @PrePersist
    void beforeInsert() {
        if (status == null) {
            status = AssignmentStatus.DRAFT;
        }
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }
    }
}
