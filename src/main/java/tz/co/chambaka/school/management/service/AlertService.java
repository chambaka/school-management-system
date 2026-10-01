package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.notification.NotificationResponse;
import tz.co.chambaka.school.management.model.InAppNotification;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.notification.NotificationChannel;
import tz.co.chambaka.school.management.notification.NotificationSettingsService;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InAppNotificationRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
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
import java.util.Optional;
import java.util.Set;

@Service
public class AlertService {

    public static final String SUBJECT_ASSIGNMENT = "ASSIGNMENT";
    public static final String SUBJECT_EXAM = "EXAM";
    public static final String SUBJECT_STUDENT = "STUDENT";
    public static final String SUBJECT_INVOICE = "INVOICE";

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final InAppNotificationRepository notificationRepository;
    private final StudentParentRepository studentParentRepository;
    private final SchoolRepository schoolRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final SmsGateway smsGateway;
    private final SmsProperties smsProperties;
    private final NotificationSettingsService notificationSettings;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public AlertService(
            InAppNotificationRepository notificationRepository,
            StudentParentRepository studentParentRepository,
            SchoolRepository schoolRepository,
            AssignmentRepository assignmentRepository,
            ExamRepository examRepository,
            StudentRepository studentRepository,
            SmsGateway smsGateway,
            SmsProperties smsProperties,
            NotificationSettingsService notificationSettings,
            UserRepository userRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.studentParentRepository = studentParentRepository;
        this.schoolRepository = schoolRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.smsGateway = smsGateway;
        this.smsProperties = smsProperties;
        this.notificationSettings = notificationSettings;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
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
        deliver(schoolId, userId, null, title, body, category, false, entityType, entityId);
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
                deliver(schoolId, user.getId(), user, title, body, category, sms, entityType, entityId);
            }
            if (includeStudents && student.getUser() != null && student.getUser().getId() != null
                    && notified.add(student.getUser().getId())) {
                deliver(schoolId, student.getUser().getId(), student.getUser(), title, body, category, false, entityType, entityId);
            }
            log.info("Email broadcast skipped (logged in-app) schoolId={} studentId={} title={}", schoolId, student.getId(), title);
        }
    }

    @Transactional
    public void remindHousehold(Long schoolId, Student student, String title, String body, Long invoiceId) {
        if (student == null || student.getId() == null) {
            return;
        }
        Set<Long> notified = new LinkedHashSet<>();
        for (StudentParent link : studentParentRepository.findByStudentId(student.getId())) {
            Parent parent = link.getParent();
            User user = parent == null ? null : parent.getUser();
            if (user == null || user.getId() == null || !notified.add(user.getId())) {
                continue;
            }
            deliverReminder(schoolId, user.getId(), user, title, body, invoiceId);
        }
        if (student.getUser() != null && student.getUser().getId() != null && notified.add(student.getUser().getId())) {
            deliverReminder(schoolId, student.getUser().getId(), student.getUser(), title, body, invoiceId);
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

    private void deliver(
            Long schoolId,
            Long userId,
            User user,
            String title,
            String body,
            String category,
            boolean smsRequested,
            String entityType,
            Long entityId
    ) {
        if (userId == null) {
            return;
        }
        Optional<NotificationChannel> choice = notificationSettings.preferredChannel(userId);
        boolean inApp = choice.isEmpty() || choice.get() == NotificationChannel.IN_APP;
        boolean text = choice.isPresent() ? choice.get() == NotificationChannel.SMS : smsRequested;
        boolean created = inApp && putNotice(schoolId, userId, title, body, category, entityType, entityId);
        if (!text || (inApp && !created)) {
            return;
        }
        User recipient = user != null ? user : userRepository.findById(userId).orElse(null);
        if (recipient == null || recipient.getPhone() == null || recipient.getPhone().isBlank()) {
            return;
        }
        sendSms(schoolId, recipient, body);
    }

    private void deliverReminder(Long schoolId, Long userId, User user, String title, String body, Long invoiceId) {
        if (userId == null) {
            return;
        }
        Optional<NotificationChannel> choice = notificationSettings.preferredChannel(userId);
        boolean inApp = choice.isEmpty() || choice.get() == NotificationChannel.IN_APP;
        boolean text = choice.isEmpty() || choice.get() == NotificationChannel.SMS;
        if (inApp) {
            putNotice(schoolId, userId, title, body, "FEES", SUBJECT_INVOICE, invoiceId);
        }
        if (!text) {
            return;
        }
        User recipient = user != null ? user : userRepository.findById(userId).orElse(null);
        if (recipient == null || recipient.getPhone() == null || recipient.getPhone().isBlank()) {
            return;
        }
        sendSms(schoolId, recipient, body);
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

    private void sendSms(Long schoolId, User user, String body) {
        String sender = schoolRepository.findById(schoolId).map(school -> school.getName()).orElse("SkuliHub");
        if (sender.length() > 11) {
            sender = sender.substring(0, 11);
        }
        try {
            smsGateway.send(PhoneNumbers.toE164Like(user.getPhone()), sender, body);
        } catch (Exception ex) {
            log.warn("SMS failed userId={}", user.getId(), ex);
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

    private boolean invoiceStillOpen(Long invoiceId, Long schoolId) {
        if (invoiceId == null || schoolId == null) {
            return false;
        }
        return invoiceRepository.findByIdAndSchoolId(invoiceId, schoolId)
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.PENDING
                        || invoice.getStatus() == InvoiceStatus.PARTIAL
                        || invoice.getStatus() == InvoiceStatus.OVERDUE)
                .filter(invoice -> invoice.getBalance() != null && invoice.getBalance().signum() > 0)
                .isPresent();
    }

    private boolean subjectExists(InAppNotification notification) {
        if (isAssignmentNotice(notification)) {
            return assignmentStillListed(notification);
        }
        String type = notification.getEntityType();
        Long entityId = notification.getEntityId();
        if (type == null || type.isBlank() || entityId == null) {
            return true;
        }
        Long schoolId = notification.getSchoolId();
        return switch (type) {
            case SUBJECT_EXAM -> examRepository.findByIdAndSchoolId(entityId, schoolId).isPresent();
            case SUBJECT_STUDENT -> studentRepository.findByIdAndSchoolId(entityId, schoolId).isPresent();
            case SUBJECT_INVOICE -> invoiceStillOpen(entityId, schoolId);
            default -> true;
        };
    }

    private boolean isAssignmentNotice(InAppNotification notification) {
        String title = notification.getTitle();
        return SUBJECT_ASSIGNMENT.equals(notification.getEntityType())
                || "ASSIGNMENT".equals(notification.getCategory())
                || "New assignment".equals(title)
                || "Assignment posted".equals(title)
                || "Assignment submitted".equals(title);
    }

    private boolean assignmentStillListed(InAppNotification notification) {
        Long schoolId = notification.getSchoolId();
        if (SUBJECT_ASSIGNMENT.equals(notification.getEntityType()) && notification.getEntityId() != null) {
            return schoolId != null && assignmentRepository.findByIdAndSchoolId(notification.getEntityId(), schoolId).isPresent();
        }
        String title = assignmentTitle(notification);
        if (schoolId == null || title == null || title.isBlank()) {
            return false;
        }
        return assignmentRepository.findBySchoolIdOrderByDueDateDesc(schoolId).stream()
                .anyMatch(assignment -> assignment.getTitle() != null && title.equals(assignment.getTitle().trim()));
    }

    private static String assignmentTitle(InAppNotification notification) {
        String body = notification.getBody();
        if (body == null) {
            return null;
        }
        String title = notification.getTitle();
        if ("New assignment".equals(title) || "Assignment posted".equals(title)) {
            int due = body.indexOf(" is due ");
            return due > 0 ? body.substring(0, due).trim() : null;
        }
        if ("Assignment submitted".equals(title)) {
            int submitted = body.lastIndexOf(" submitted ");
            return submitted >= 0 ? body.substring(submitted + " submitted ".length()).trim() : null;
        }
        return null;
    }
}
