package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findBySchoolIdOrderByNameAsc(Long schoolId);

    Optional<Building> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Building> findBySchoolIdAndNameIgnoreCase(Long schoolId, String name);

    boolean existsBySchoolIdAndNameIgnoreCase(Long schoolId, String name);
}
