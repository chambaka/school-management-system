package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findBySchoolIdOrderByNameAsc(Long schoolId);

    Optional<Subject> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolIdAndCodeIgnoreCase(Long schoolId, String code);
}
