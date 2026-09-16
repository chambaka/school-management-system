package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.BellPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BellPeriodRepository extends JpaRepository<BellPeriod, Long> {

    List<BellPeriod> findBySchoolIdOrderBySortOrderAsc(Long schoolId);

    Optional<BellPeriod> findByIdAndSchoolId(Long id, Long schoolId);
}
