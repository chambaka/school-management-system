package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    List<FeeStructure> findBySchoolId(Long schoolId);

    List<FeeStructure> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);

    List<FeeStructure> findBySchoolIdAndAcademicYearIdAndSchoolClassId(Long schoolId, Long academicYearId, Long schoolClassId);

    Optional<FeeStructure> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolClassId(Long schoolClassId);
}
