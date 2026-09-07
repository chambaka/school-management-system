package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.StudentParent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {

    List<StudentParent> findByStudentId(Long studentId);

    List<StudentParent> findByParentId(Long parentId);

    boolean existsByStudentId(Long studentId);

    boolean existsByStudentIdAndParentId(Long studentId, Long parentId);

    Optional<StudentParent> findByStudentIdAndParentId(Long studentId, Long parentId);
}
