package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findBySchoolIdOrderByNameAsc(Long schoolId);

    Optional<Classroom> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);

    boolean existsBySchoolIdAndCodeIgnoreCase(Long schoolId, String code);
}
