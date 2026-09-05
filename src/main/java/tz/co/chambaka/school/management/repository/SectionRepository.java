package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findBySchoolIdAndSchoolClassIdOrderByNameAsc(Long schoolId, Long schoolClassId);

    Optional<Section> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolClassIdAndNameIgnoreCase(Long schoolClassId, String name);
}
