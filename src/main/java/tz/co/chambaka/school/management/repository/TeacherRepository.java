package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Page<Teacher> findBySchoolId(Long schoolId, Pageable pageable);

    Optional<Teacher> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Teacher> findByUserId(Long userId);

    boolean existsBySchoolIdAndEmployeeIdIgnoreCase(Long schoolId, String employeeId);
}
