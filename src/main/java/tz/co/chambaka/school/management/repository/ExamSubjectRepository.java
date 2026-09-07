package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {

    List<ExamSubject> findByExamId(Long examId);

    Optional<ExamSubject> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<ExamSubject> findByExamIdAndSubjectId(Long examId, Long subjectId);

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);

    boolean existsBySubjectId(Long subjectId);
}
