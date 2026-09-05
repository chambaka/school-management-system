package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampusRepository extends JpaRepository<Campus, Long> {

    List<Campus> findBySchoolIdOrderByPrimaryCampusDescNameAsc(Long schoolId);

    List<Campus> findByTenantIdOrderBySchoolIdAscNameAsc(Long tenantId);

    Optional<Campus> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Campus> findFirstBySchoolIdAndPrimaryCampusTrue(Long schoolId);

    boolean existsBySchoolIdAndCodeIgnoreCase(Long schoolId, String code);

    long countBySchoolId(Long schoolId);
}
