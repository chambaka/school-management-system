package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {

    List<TeacherAttendance> findBySchoolIdAndAttendanceDate(Long schoolId, LocalDate date);

    List<TeacherAttendance> findBySchoolIdAndTeacherIdAndAttendanceDateBetween(
            Long schoolId, Long teacherId, LocalDate start, LocalDate end);

    Optional<TeacherAttendance> findByTeacherIdAndAttendanceDate(Long teacherId, LocalDate date);
}
