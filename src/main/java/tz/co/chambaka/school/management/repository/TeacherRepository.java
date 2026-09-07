package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.enums.TeacherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Page<Teacher> findBySchoolId(Long schoolId, Pageable pageable);

    @Query("""
            select t from Teacher t
            where t.schoolId = :schoolId
              and (
                (:archived = true and t.status = :archivedStatus)
                or (:archived = false and (t.status is null or t.status <> :archivedStatus))
              )
            """)
    Page<Teacher> search(
            @Param("schoolId") Long schoolId,
            @Param("archived") boolean archived,
            @Param("archivedStatus") TeacherStatus archivedStatus,
            Pageable pageable
    );

    Optional<Teacher> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Teacher> findByUserId(Long userId);

    boolean existsBySchoolIdAndEmployeeIdIgnoreCase(Long schoolId, String employeeId);
}
