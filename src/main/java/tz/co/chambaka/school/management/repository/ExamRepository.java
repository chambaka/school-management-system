package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findBySchoolIdAndAcademicYearIdOrderByStartDateDesc(Long schoolId, Long academicYearId);

    List<Exam> findBySchoolIdOrderByStartDateDesc(Long schoolId);

    Optional<Exam> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolClassId(Long schoolClassId);
}
