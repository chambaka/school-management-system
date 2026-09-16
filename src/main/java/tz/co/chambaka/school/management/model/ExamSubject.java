package tz.co.chambaka.school.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "exam_subjects")
public class ExamSubject extends TenantEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal maxMarks;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal passMarks;

    private LocalDate examDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(length = 80)
    private String venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invigilator_id")
    private Teacher invigilator;
}
