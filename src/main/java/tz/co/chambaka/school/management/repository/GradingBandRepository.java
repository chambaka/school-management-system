package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.GradingBand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradingBandRepository extends JpaRepository<GradingBand, Long> {

    List<GradingBand> findBySchoolIdOrderBySortOrderAsc(Long schoolId);
}
