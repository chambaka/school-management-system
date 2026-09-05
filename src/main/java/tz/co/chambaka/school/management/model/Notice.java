package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.NoticeAudience;
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

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notices")
public class Notice extends TenantEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoticeAudience audience = NoticeAudience.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Column(nullable = false)
    private boolean published = false;

    private Instant publishAt;

    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
