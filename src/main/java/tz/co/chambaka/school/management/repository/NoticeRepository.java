package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Notice;
import tz.co.chambaka.school.management.model.enums.NoticeAudience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    List<Notice> findBySchoolIdAndPublishedTrueOrderByCreatedAtDesc(Long schoolId);

    List<Notice> findBySchoolIdAndPublishedTrueAndAudienceInOrderByCreatedAtDesc(
            Long schoolId, List<NoticeAudience> audiences);

    Optional<Notice> findByIdAndSchoolId(Long id, Long schoolId);

    List<Notice> findBySchoolClassId(Long schoolClassId);
}
