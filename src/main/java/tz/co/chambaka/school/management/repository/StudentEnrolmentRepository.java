package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.StudentEnrolment;
import tz.co.chambaka.school.management.model.enums.PromotionAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentEnrolmentRepository extends JpaRepository<StudentEnrolment, Long> {

    List<StudentEnrolment> findBySchoolIdAndStudentIdOrderByEffectiveDateDesc(Long schoolId, Long studentId);

    @Query("""
            select distinct e from StudentEnrolment e
            join fetch e.student s
            left join fetch s.user
            join fetch e.academicYear
            left join fetch e.schoolClass
            left join fetch e.section
            where e.schoolId = :schoolId
              and (:studentId is null or s.id = :studentId)
              and (:academicYearId is null or e.academicYear.id = :academicYearId)
              and (:action is null or e.action = :action)
            order by e.effectiveDate desc, e.id desc
            """)
    List<StudentEnrolment> search(
            @Param("schoolId") Long schoolId,
            @Param("studentId") Long studentId,
            @Param("academicYearId") Long academicYearId,
            @Param("action") PromotionAction action
    );

    boolean existsByAcademicYearId(Long academicYearId);
}
