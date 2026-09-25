package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "teacher_qualifications")
public class TeacherQualification extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false)
    private int sortOrder = 0;
}
