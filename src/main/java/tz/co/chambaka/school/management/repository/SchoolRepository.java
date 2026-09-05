package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findBySlug(String slug);

    Optional<School> findByCustomDomain(String customDomain);

    boolean existsBySlug(String slug);

    List<School> findByTenantIdOrderByNameAsc(Long tenantId);

    long countByTenantId(Long tenantId);

    Optional<School> findByIdAndTenantId(Long id, Long tenantId);
}
