package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.ResultWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultWeightConfigRepository extends JpaRepository<ResultWeightConfig, Long> {

    List<ResultWeightConfig> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);

    Optional<ResultWeightConfig> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<ResultWeightConfig> findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIdAndSubjectId(
            Long schoolId, Long academicYearId, Long academicTermId, Long subjectId);

    Optional<ResultWeightConfig> findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIsNullAndSubjectIsNull(
            Long schoolId, Long academicYearId);
}
