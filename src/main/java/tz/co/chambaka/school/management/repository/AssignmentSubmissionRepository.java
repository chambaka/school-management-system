package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDescIdDesc(Long assignmentId);

    List<AssignmentSubmission> findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(
            Long assignmentId,
            Long studentId
    );

    Optional<AssignmentSubmission> findByIdAndAssignment_IdAndStudent_IdAndSchoolId(
            Long id,
            Long assignmentId,
            Long studentId,
            Long schoolId
    );

    @Query("""
            select s from AssignmentSubmission s
            join fetch s.assignment a
            join fetch s.student st
            join fetch st.user
            where st.id = :studentId
            """)
    List<AssignmentSubmission> findMine(@Param("studentId") Long studentId);
}
