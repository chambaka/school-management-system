package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findBySchoolIdAndAcademicYearIdOrderByDisplayOrderAsc(Long schoolId, Long academicYearId);

    List<SchoolClass> findBySchoolIdOrderByDisplayOrderAsc(Long schoolId);

    Optional<SchoolClass> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCase(Long schoolId, Long academicYearId, String code);
}
