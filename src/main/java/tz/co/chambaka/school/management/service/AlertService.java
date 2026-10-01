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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        putNotice(schoolId, userId, title, body, category, entityType, entityId);
    }

    @Transactional
    public void notifyParentsOfStudent(Long schoolId, Student student, String title, String body, String category, boolean sms) {
        notifyParentsOfStudent(schoolId, student, title, body, category, sms, null, null, true);
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
        notifyParentsOfStudent(schoolId, student, title, body, category, sms, entityType, entityId, true);
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
            Long entityId,
            boolean includeStudent
    ) {
        notifyHouseholds(schoolId, List.of(student), title, body, category, sms, entityType, entityId, includeStudent);
    }

    @Transactional
    public void notifyHouseholds(
            Long schoolId,
            List<Student> students,
            String title,
            String body,
            String category,
            boolean sms,
            String entityType,
            Long entityId,
            boolean includeStudents
    ) {
        if (students == null || students.isEmpty()) {
            return;
        }
        Set<Long> notified = new LinkedHashSet<>();
        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            for (StudentParent link : studentParentRepository.findByStudentId(student.getId())) {
                Parent parent = link.getParent();
                User user = parent == null ? null : parent.getUser();
                if (user == null || user.getId() == null || !notified.add(user.getId())) {
                    continue;
                }
                boolean created = putNotice(schoolId, user.getId(), title, body, category, entityType, entityId);
                if (created && sms && user.getPhone() != null) {
                    sendParentSms(schoolId, parent, student, user, body);
                }
            }
            if (includeStudents && student.getUser() != null && student.getUser().getId() != null
                    && notified.add(student.getUser().getId())) {
                putNotice(schoolId, student.getUser().getId(), title, body, category, entityType, entityId);
            }
            log.info("Email broadcast skipped (logged in-app) schoolId={} studentId={} title={}", schoolId, student.getId(), title);
        }
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
        List<InAppNotification> unique = new ArrayList<>();
        Set<String> seenUnread = new HashSet<>();
        List<InAppNotification> duplicates = new ArrayList<>();
        for (InAppNotification notification : live) {
            String key = unreadKey(notification);
            if (key != null && !seenUnread.add(key)) {
                duplicates.add(notification);
                continue;
            }
            unique.add(notification);
        }
        if (!duplicates.isEmpty()) {
            notificationRepository.deleteAll(duplicates);
        }
        return unique.stream()
                .map(n -> new NotificationResponse(
                        n.getId(), n.getTitle(), n.getBody(), n.getCategory(), n.isReadFlag(),
                        n.getCreatedAt() == null ? null : n.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        notificationRepository.findByIdAndUserId(id, userId).ifPresent(n -> n.setReadFlag(true));
    }

    private boolean putNotice(
            Long schoolId,
            Long userId,
            String title,
            String body,
            String category,
            String entityType,
            Long entityId
    ) {
        if (userId == null) {
            return false;
        }
        if (!eachSubmission(title) && entityType != null && !entityType.isBlank() && entityId != null && title != null) {
            List<InAppNotification> existing = notificationRepository
                    .findBySchoolIdAndUserIdAndEntityTypeAndEntityIdAndTitleAndReadFlagFalseOrderByIdAsc(
                            schoolId, userId, entityType, entityId, title);
            if (!existing.isEmpty()) {
                InAppNotification keep = existing.getFirst();
                keep.setBody(body);
                keep.setCategory(category);
                notificationRepository.save(keep);
                if (existing.size() > 1) {
                    notificationRepository.deleteAll(existing.subList(1, existing.size()));
                }
                return false;
            }
        }
        InAppNotification notification = new InAppNotification();
        notification.setSchoolId(schoolId);
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCategory(category);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notificationRepository.save(notification);
        return true;
    }

    private void sendParentSms(Long schoolId, Parent parent, Student student, User user, String body) {
        String sender = schoolRepository.findById(schoolId).map(school -> school.getName()).orElse("SkuliHub");
        if (sender.length() > 11) {
            sender = sender.substring(0, 11);
        }
        try {
            smsGateway.send(PhoneNumbers.toE164Like(user.getPhone()), sender, body);
        } catch (Exception ex) {
            log.warn("SMS failed parentId={} studentId={}", parent.getId(), student.getId(), ex);
        }
    }

    private static boolean eachSubmission(String title) {
        return "Assignment submitted".equals(title);
    }

    private static String unreadKey(InAppNotification notification) {
        if (notification.isReadFlag() || notification.getTitle() == null || eachSubmission(notification.getTitle())) {
            return null;
        }
        if (notification.getEntityType() != null
                && !notification.getEntityType().isBlank()
                && notification.getEntityId() != null) {
            return notification.getEntityType() + ":" + notification.getEntityId() + ":" + notification.getTitle();
        }
        String body = notification.getBody() == null ? "" : notification.getBody();
        return "copy:" + notification.getTitle() + ":" + body;
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
