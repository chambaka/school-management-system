package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.enums.ParentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    Page<Parent> findBySchoolId(Long schoolId, Pageable pageable);

    @Query("""
            select p from Parent p
            where p.schoolId = :schoolId
              and (
                (:archived = true and p.status = :archivedStatus)
                or (:archived = false and (p.status is null or p.status <> :archivedStatus))
              )
            """)
    Page<Parent> search(
            @Param("schoolId") Long schoolId,
            @Param("archived") boolean archived,
            @Param("archivedStatus") ParentStatus archivedStatus,
            Pageable pageable
    );

    Optional<Parent> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Parent> findByUserId(Long userId);
}
