package tz.co.chambaka.school.management.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_permissions")
public class AppPermission {

    @Id
    @Column(length = 80)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;
}
