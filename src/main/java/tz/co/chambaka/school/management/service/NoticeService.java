package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.notice.NoticeRequest;
import tz.co.chambaka.school.management.dto.notice.NoticeResponse;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Notice;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.NoticeAudience;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.NoticeRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final ClassService classService;

    public NoticeService(NoticeRepository noticeRepository, UserRepository userRepository, ClassService classService) {
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.classService = classService;
    }

    @Transactional
    public NoticeResponse create(Long schoolId, NoticeRequest request, Long createdBy) {
        Notice notice = new Notice();
        notice.setSchoolId(schoolId);
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setAudience(request.audience());
        notice.setPublished(request.published());
        notice.setPublishAt(request.publishAt());
        notice.setExpiresAt(request.expiresAt());
        notice.setSchoolClass(request.schoolClassId() == null ? null : classService.require(schoolId, request.schoolClassId()));
        notice.setCreatedBy(userRepository.findById(createdBy).orElse(null));
        Notice saved = noticeRepository.save(notice);
        log.info("Created notice id={} schoolId={} published={}", saved.getId(), schoolId, saved.isPublished());
        return toResponse(saved);
    }

    @Transactional
    public NoticeResponse update(Long schoolId, Long id, NoticeRequest request) {
        Notice notice = require(schoolId, id);
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setAudience(request.audience());
        notice.setPublished(request.published());
        notice.setPublishAt(request.publishAt());
        notice.setExpiresAt(request.expiresAt());
        notice.setSchoolClass(request.schoolClassId() == null ? null : classService.require(schoolId, request.schoolClassId()));
        return toResponse(notice);
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> listForAdmin(Long schoolId) {
        return noticeRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> listForAudience(Long schoolId, Role role) {
        List<NoticeAudience> audiences = switch (role) {
            case TEACHER -> List.of(NoticeAudience.ALL, NoticeAudience.TEACHERS, NoticeAudience.STAFF);
            case STUDENT -> List.of(NoticeAudience.ALL, NoticeAudience.STUDENTS);
            case PARENT -> List.of(NoticeAudience.ALL, NoticeAudience.PARENTS);
            default -> List.of(NoticeAudience.ALL, NoticeAudience.STAFF, NoticeAudience.TEACHERS,
                    NoticeAudience.STUDENTS, NoticeAudience.PARENTS);
        };
        return noticeRepository.findBySchoolIdAndPublishedTrueAndAudienceInOrderByCreatedAtDesc(schoolId, audiences)
                .stream().map(this::toResponse).toList();
    }

    public Notice require(Long schoolId, Long id) {
        return noticeRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notice", id));
    }

    private NoticeResponse toResponse(Notice notice) {
        SchoolClass schoolClass = notice.getSchoolClass();
        User creator = notice.getCreatedBy();
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAudience(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                notice.isPublished(),
                notice.getPublishAt(),
                notice.getExpiresAt(),
                creator != null ? creator.getName() : null
        );
    }
}
