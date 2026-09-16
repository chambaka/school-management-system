package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findBySchoolIdOrderByDueDateDesc(Long schoolId);

    List<Assignment> findBySchoolIdAndSchoolClassIdOrderByDueDateDesc(Long schoolId, Long schoolClassId);

    Optional<Assignment> findByIdAndSchoolId(Long id, Long schoolId);
}
