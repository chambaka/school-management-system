package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TimetableLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimetableLockRepository extends JpaRepository<TimetableLock, Long> {

    Optional<TimetableLock> findBySchoolIdAndSectionIdAndAcademicYearId(Long schoolId, Long sectionId, Long academicYearId);

    boolean existsByAcademicYearId(Long academicYearId);
}
