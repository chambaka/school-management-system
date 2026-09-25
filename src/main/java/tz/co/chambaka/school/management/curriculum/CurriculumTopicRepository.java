package tz.co.chambaka.school.management.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurriculumTopicRepository extends JpaRepository<CurriculumTopic, Long> {

    List<CurriculumTopic> findBySchoolIdAndSubjectIdOrderBySortOrderAscTitleAsc(Long schoolId, Long subjectId);

    List<CurriculumTopic> findBySchoolIdOrderBySortOrderAscTitleAsc(Long schoolId);

    Optional<CurriculumTopic> findByIdAndSchoolId(Long id, Long schoolId);
}
