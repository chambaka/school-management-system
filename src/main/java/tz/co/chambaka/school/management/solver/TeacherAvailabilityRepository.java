package tz.co.chambaka.school.management.solver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, Long> {

    List<TeacherAvailability> findBySchoolIdAndTeacherId(Long schoolId, Long teacherId);

    List<TeacherAvailability> findBySchoolIdOrderByDayOfWeekAscStartTimeAsc(Long schoolId);

    List<TeacherAvailability> findBySchoolIdAndTeacherIdAndDayOfWeek(Long schoolId, Long teacherId, DayOfWeek day);

    Optional<TeacherAvailability> findByIdAndSchoolId(Long id, Long schoolId);
}
