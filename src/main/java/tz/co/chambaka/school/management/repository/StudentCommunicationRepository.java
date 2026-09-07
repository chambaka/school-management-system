package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.StudentCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StudentCommunicationRepository extends JpaRepository<StudentCommunication, Long> {

    List<StudentCommunication> findBySchoolIdAndStudentIdOrderByCreatedAtAsc(Long schoolId, Long studentId);

    List<StudentCommunication> findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(Long schoolId, Collection<Long> studentIds);
}
