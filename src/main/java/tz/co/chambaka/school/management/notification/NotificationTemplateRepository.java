package tz.co.chambaka.school.management.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    List<NotificationTemplate> findBySchoolIdOrderByEventKeyAsc(Long schoolId);

    Optional<NotificationTemplate> findBySchoolIdAndEventKeyAndChannel(
            Long schoolId, String eventKey, NotificationChannel channel);

    Optional<NotificationTemplate> findByIdAndSchoolId(Long id, Long schoolId);
}
