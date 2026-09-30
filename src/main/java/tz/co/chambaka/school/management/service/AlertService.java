package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.notification.NotificationResponse;
import tz.co.chambaka.school.management.model.InAppNotification;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InAppNotificationRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import tz.co.chambaka.school.management.sms.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    public static final String SUBJECT_ASSIGNMENT = "ASSIGNMENT";
    public static final String SUBJECT_EXAM = "EXAM";
    public static final String SUBJECT_STUDENT = "STUDENT";

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final InAppNotificationRepository notificationRepository;
    private final StudentParentRepository studentParentRepository;
    private final SchoolRepository schoolRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final SmsGateway smsGateway;
    private final SmsProperties smsProperties;

    public AlertService(
            InAppNotificationRepository notificationRepository,
            StudentParentRepository studentParentRepository,
            SchoolRepository schoolRepository,
            AssignmentRepository assignmentRepository,
            ExamRepository examRepository,
            StudentRepository studentRepository,
            SmsGateway smsGateway,
            SmsProperties smsProperties
    ) {
        this.notificationRepository = notificationRepository;
        this.studentParentRepository = studentParentRepository;
        this.schoolRepository = schoolRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.smsGateway = smsGateway;
        this.smsProperties = smsProperties;
    }

    @Transactional
    public void notifyUser(Long schoolId, Long userId, String title, String body, String category) {
        notifyUser(schoolId, userId, title, body, category, null, null);
    }

    @Transactional
    public void notifyUser(
            Long schoolId,
            Long userId,
            String title,
            String body,
            String category,
            String entityType,
            Long entityId
    ) {
        InAppNotification notification = new InAppNotification();
        notification.setSchoolId(schoolId);
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCategory(category);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyParentsOfStudent(Long schoolId, Student student, String title, String body, String category, boolean sms) {
        notifyParentsOfStudent(schoolId, student, title, body, category, sms, null, null);
    }

    @Transactional
    public void notifyParentsOfStudent(
            Long schoolId,
            Student student,
            String title,
            String body,
            String category,
            boolean sms,
            String entityType,
            Long entityId
    ) {
        List<StudentParent> links = studentParentRepository.findByStudentId(student.getId());
        for (StudentParent link : links) {
            Parent parent = link.getParent();
            User user = parent.getUser();
            notifyUser(schoolId, user.getId(), title, body, category, entityType, entityId);
            if (sms && user.getPhone() != null) {
                String sender = schoolRepository.findById(schoolId).map(s -> s.getName()).orElse("SkuliHub");
                if (sender.length() > 11) {
                    sender = sender.substring(0, 11);
                }
                try {
                    smsGateway.send(PhoneNumbers.toE164Like(user.getPhone()), sender, body);
                } catch (Exception ex) {
                    log.warn("SMS failed parentId={} studentId={}", parent.getId(), student.getId(), ex);
                }
            }
        }
        if (student.getUser() != null) {
            notifyUser(schoolId, student.getUser().getId(), title, body, category, entityType, entityId);
        }
        log.info("Email broadcast skipped (logged in-app) schoolId={} studentId={} title={}", schoolId, student.getId(), title);
    }

    @Transactional
    public void removeForEntity(Long schoolId, String entityType, Long entityId) {
        if (entityType == null || entityType.isBlank() || entityId == null) {
            return;
        }
        notificationRepository.deleteBySchoolIdAndEntityTypeAndEntityId(schoolId, entityType, entityId);
    }

    @Transactional
    public List<NotificationResponse> inbox(Long schoolId, Long userId) {
        List<InAppNotification> all = notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(schoolId, userId);
        List<InAppNotification> live = new ArrayList<>();
        List<InAppNotification> stale = new ArrayList<>();
        for (InAppNotification notification : all) {
            if (subjectExists(notification)) {
                live.add(notification);
            } else {
                stale.add(notification);
            }
        }
        if (!stale.isEmpty()) {
            notificationRepository.deleteAll(stale);
        }
        return live.stream()
                .map(n -> new NotificationResponse(
                        n.getId(), n.getTitle(), n.getBody(), n.getCategory(), n.isReadFlag(),
                        n.getCreatedAt() == null ? null : n.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        notificationRepository.findByIdAndUserId(id, userId).ifPresent(n -> n.setReadFlag(true));
    }

    private boolean subjectExists(InAppNotification notification) {
        String type = notification.getEntityType();
        Long entityId = notification.getEntityId();
        if (type == null || type.isBlank() || entityId == null) {
            return true;
        }
        Long schoolId = notification.getSchoolId();
        return switch (type) {
            case SUBJECT_ASSIGNMENT -> assignmentRepository.findByIdAndSchoolId(entityId, schoolId).isPresent();
            case SUBJECT_EXAM -> examRepository.findByIdAndSchoolId(entityId, schoolId).isPresent();
            case SUBJECT_STUDENT -> studentRepository.findByIdAndSchoolId(entityId, schoolId).isPresent();
            default -> true;
        };
    }
}
