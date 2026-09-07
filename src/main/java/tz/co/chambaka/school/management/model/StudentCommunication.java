package tz.co.chambaka.school.management.model;

import tz.co.chambaka.school.management.model.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "student_communications")
public class StudentCommunication extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean notifyParentsSms;

    @OneToMany(mappedBy = "communication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SmsDelivery> smsDeliveries = new ArrayList<>();
}
