package tz.co.chambaka.school.management.solver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, Long> {

    List<TeacherAvailability> findBySchoolIdAndTeacherId(Long schoolId, Long teacherId);

    List<TeacherAvailability> findBySchoolIdAndTeacherIdAndDayOfWeek(Long schoolId, Long teacherId, DayOfWeek day);
}
