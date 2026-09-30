package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AssignmentSubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionAttachmentRepository extends JpaRepository<AssignmentSubmissionAttachment, Long> {

    List<AssignmentSubmissionAttachment> findBySubmission_IdOrderByIdAsc(Long submissionId);

    List<AssignmentSubmissionAttachment> findBySubmission_IdInOrderByIdAsc(Collection<Long> submissionIds);

    Optional<AssignmentSubmissionAttachment> findByIdAndSubmission_IdAndSchoolId(
            Long id,
            Long submissionId,
            Long schoolId
    );
}
