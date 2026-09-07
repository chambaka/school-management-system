package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Page<Student> findBySchoolId(Long schoolId, Pageable pageable);

    Page<Student> findBySchoolIdAndSchoolClassId(Long schoolId, Long schoolClassId, Pageable pageable);

    @Query("""
            select s from Student s
            where s.schoolId = :schoolId
              and (:classId is null or s.schoolClass.id = :classId)
              and (
                (:archived = true and s.status = :archivedStatus)
                or (:archived = false and (s.status is null or s.status <> :archivedStatus))
              )
            """)
    Page<Student> search(
            @Param("schoolId") Long schoolId,
            @Param("classId") Long classId,
            @Param("archived") boolean archived,
            @Param("archivedStatus") StudentStatus archivedStatus,
            Pageable pageable
    );

    List<Student> findBySchoolIdAndSchoolClassId(Long schoolId, Long schoolClassId);

    List<Student> findBySchoolIdAndSectionId(Long schoolId, Long sectionId);

    Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Student> findByUserId(Long userId);

    boolean existsBySchoolIdAndAdmissionNoIgnoreCase(Long schoolId, String admissionNo);

    long countBySchoolIdAndAdmissionNoStartingWithIgnoreCase(Long schoolId, String prefix);

    long countBySchoolClassId(Long schoolClassId);

    long countBySectionId(Long sectionId);
}
