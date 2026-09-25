package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.LessonLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LessonLogRepository extends JpaRepository<LessonLog, Long> {

    List<LessonLog> findBySchoolIdAndSectionIdAndLessonDate(Long schoolId, Long sectionId, LocalDate lessonDate);

    List<LessonLog> findBySchoolIdAndTeacherIdOrderByLessonDateDesc(Long schoolId, Long teacherId);

    Optional<LessonLog> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByCurriculumTopicId(Long curriculumTopicId);
}
