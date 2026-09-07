package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByExamIdAndSchoolId(Long examId, Long schoolId);

    List<Grade> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<Grade> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByExamIdAndStudentIdAndSubjectId(Long examId, Long studentId, Long subjectId);

    boolean existsBySubjectId(Long subjectId);
}
