package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.notification.NotificationResponse;
import tz.co.chambaka.school.management.model.InAppNotification;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.repository.InAppNotificationRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import tz.co.chambaka.school.management.sms.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final InAppNotificationRepository notificationRepository;
    private final StudentParentRepository studentParentRepository;
    private final SchoolRepository schoolRepository;
    private final SmsGateway smsGateway;
    private final SmsProperties smsProperties;

    public AlertService(
            InAppNotificationRepository notificationRepository,
            StudentParentRepository studentParentRepository,
            SchoolRepository schoolRepository,
            SmsGateway smsGateway,
            SmsProperties smsProperties
    ) {
        this.notificationRepository = notificationRepository;
        this.studentParentRepository = studentParentRepository;
        this.schoolRepository = schoolRepository;
        this.smsGateway = smsGateway;
        this.smsProperties = smsProperties;
    }

    @Transactional
    public void notifyUser(Long schoolId, Long userId, String title, String body, String category) {
        InAppNotification notification = new InAppNotification();
        notification.setSchoolId(schoolId);
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCategory(category);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyParentsOfStudent(Long schoolId, Student student, String title, String body, String category, boolean sms) {
        List<StudentParent> links = studentParentRepository.findByStudentId(student.getId());
        for (StudentParent link : links) {
            Parent parent = link.getParent();
            User user = parent.getUser();
            notifyUser(schoolId, user.getId(), title, body, category);
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
            notifyUser(schoolId, student.getUser().getId(), title, body, category);
        }
        log.info("Email broadcast skipped (logged in-app) schoolId={} studentId={} title={}", schoolId, student.getId(), title);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> inbox(Long schoolId, Long userId) {
        return notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(schoolId, userId).stream()
                .map(n -> new NotificationResponse(
                        n.getId(), n.getTitle(), n.getBody(), n.getCategory(), n.isReadFlag(),
                        n.getCreatedAt() == null ? null : n.getCreatedAt().toString()))
                .toList();
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        notificationRepository.findByIdAndUserId(id, userId).ifPresent(n -> n.setReadFlag(true));
    }
}
