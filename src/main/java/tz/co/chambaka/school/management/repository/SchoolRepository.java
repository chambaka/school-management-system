package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findBySlug(String slug);

    Optional<School> findByCustomDomain(String customDomain);

    boolean existsBySlug(String slug);
}
