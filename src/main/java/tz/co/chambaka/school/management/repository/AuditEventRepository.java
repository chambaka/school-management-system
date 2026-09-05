package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    List<AuditEvent> findByCorrectionIdOrderByCreatedAtAsc(String correctionId);

    List<AuditEvent> findByCorrectionIdAndSchoolIdOrderByCreatedAtAsc(String correctionId, Long schoolId);
}
