package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findBySchoolIdOrderByNameAsc(Long schoolId);

    Optional<Department> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Department> findBySchoolIdAndNameIgnoreCase(Long schoolId, String name);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);
}
