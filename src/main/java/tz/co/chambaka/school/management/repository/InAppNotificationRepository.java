package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findBySchoolIdAndUserIdOrderByCreatedAtDesc(Long schoolId, Long userId);

    Optional<InAppNotification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);

    void deleteBySchoolIdAndEntityTypeAndEntityId(Long schoolId, String entityType, Long entityId);
}
