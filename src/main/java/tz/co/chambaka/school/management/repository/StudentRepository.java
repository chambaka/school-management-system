package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Page<Student> findBySchoolId(Long schoolId, Pageable pageable);

    Page<Student> findBySchoolIdAndSchoolClassId(Long schoolId, Long schoolClassId, Pageable pageable);

    List<Student> findBySchoolIdAndSchoolClassId(Long schoolId, Long schoolClassId);

    List<Student> findBySchoolIdAndSectionId(Long schoolId, Long sectionId);

    Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Student> findByUserId(Long userId);

    boolean existsBySchoolIdAndAdmissionNoIgnoreCase(Long schoolId, String admissionNo);

    long countBySchoolClassId(Long schoolClassId);

    long countBySectionId(Long sectionId);
}
