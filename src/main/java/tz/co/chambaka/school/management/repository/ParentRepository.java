package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Parent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    Page<Parent> findBySchoolId(Long schoolId, Pageable pageable);

    Optional<Parent> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Parent> findByUserId(Long userId);
}
