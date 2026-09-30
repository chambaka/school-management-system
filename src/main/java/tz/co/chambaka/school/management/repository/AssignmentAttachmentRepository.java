package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, Long> {

    List<AssignmentAttachment> findByAssignment_IdOrderByIdAsc(Long assignmentId);

    List<AssignmentAttachment> findByAssignment_IdInOrderByIdAsc(Collection<Long> assignmentIds);

    Optional<AssignmentAttachment> findByIdAndAssignment_IdAndSchoolId(Long id, Long assignmentId, Long schoolId);
}
