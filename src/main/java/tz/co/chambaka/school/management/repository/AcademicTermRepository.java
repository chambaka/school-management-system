package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {

    List<AcademicTerm> findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(Long schoolId, Long academicYearId);

    List<AcademicTerm> findBySchoolIdOrderByStartDateDesc(Long schoolId);

    Optional<AcademicTerm> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolIdAndAcademicYearIdAndNameIgnoreCase(Long schoolId, Long academicYearId, String name);

    long countByAcademicYearId(Long academicYearId);
}
