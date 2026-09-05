package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "campus_id")
    private Long campusId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    private Instant lastLoginAt;
}
