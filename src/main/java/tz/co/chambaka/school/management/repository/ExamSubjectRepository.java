package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {

    List<ExamSubject> findByExamId(Long examId);

    @Query("""
            select distinct p from ExamSubject p
            join fetch p.exam
            join fetch p.subject
            left join fetch p.invigilator i
            left join fetch i.user
            where p.exam.id in :examIds
            """)
    List<ExamSubject> findByExamIdIn(@Param("examIds") Collection<Long> examIds);

    Optional<ExamSubject> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<ExamSubject> findByExamIdAndSubjectId(Long examId, Long subjectId);

    boolean existsByExamIdAndSubjectId(Long examId, Long subjectId);

    boolean existsBySubjectId(Long subjectId);

    void deleteByExamId(Long examId);
}
