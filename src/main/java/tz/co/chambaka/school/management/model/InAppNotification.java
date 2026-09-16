package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "in_app_notifications")
public class InAppNotification extends TenantEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false, length = 40)
    private String category = "GENERAL";

    @Column(nullable = false)
    private boolean readFlag = false;
}
