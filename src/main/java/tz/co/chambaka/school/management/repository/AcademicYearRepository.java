package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(Long schoolId);

    Optional<AcademicYear> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<AcademicYear> findBySchoolIdAndCurrentYearTrue(Long schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);
}
