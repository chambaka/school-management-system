package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TeacherQualification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherQualificationRepository extends JpaRepository<TeacherQualification, Long> {

    List<TeacherQualification> findAllByOrderBySortOrderAscNameAsc();

    Optional<TeacherQualification> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
