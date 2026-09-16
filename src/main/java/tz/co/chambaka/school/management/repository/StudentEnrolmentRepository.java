package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.StudentEnrolment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentEnrolmentRepository extends JpaRepository<StudentEnrolment, Long> {

    List<StudentEnrolment> findBySchoolIdAndStudentIdOrderByEffectiveDateDesc(Long schoolId, Long studentId);
}
