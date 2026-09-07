package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.StudentAttendance;
import tz.co.chambaka.school.management.model.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

    List<StudentAttendance> findBySchoolIdAndSectionIdAndAttendanceDate(Long schoolId, Long sectionId, LocalDate date);

    List<StudentAttendance> findBySchoolIdAndStudentIdAndAttendanceDateBetween(
            Long schoolId, Long studentId, LocalDate start, LocalDate end);

    Optional<StudentAttendance> findByStudentIdAndAttendanceDate(Long studentId, LocalDate date);

    long countByStudentIdAndAttendanceDateBetweenAndStatus(
            Long studentId, LocalDate start, LocalDate end, AttendanceStatus status);

    long countByStudentIdAndAttendanceDateBetween(Long studentId, LocalDate start, LocalDate end);

    void deleteBySectionId(Long sectionId);
}
