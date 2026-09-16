package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.model.InAppNotification;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.repository.InAppNotificationRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.sms.SmsGateway;
import tz.co.chambaka.school.management.support.Fixtures;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock InAppNotificationRepository notificationRepository;
    @Mock StudentParentRepository studentParentRepository;
    @Mock SchoolRepository schoolRepository;
    @Mock SmsGateway smsGateway;
    @Mock SmsProperties smsProperties;
    @InjectMocks AlertService service;

    @Test
    void storesNotificationsAndMapsInboxWithOptionalTimestamp() {
        service.notifyUser(1L, 4L, "Title", "Body", "RESULTS");
        verify(notificationRepository).save(any(InAppNotification.class));

        InAppNotification first = notification(1L, false);
        first.setCreatedAt(Instant.parse("2026-09-16T10:00:00Z"));
        InAppNotification second = notification(2L, true);
        when(notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(1L, 4L))
                .thenReturn(List.of(first, second));
        assertThat(service.inbox(1L, 4L)).hasSize(2);
        assertThat(service.inbox(1L, 4L).getFirst().createdAt()).isEqualTo("2026-09-16T10:00:00Z");
        assertThat(service.inbox(1L, 4L).get(1).createdAt()).isNull();
    }

    @Test
    void notifiesParentsAndStudentAndSendsNormalizedSms() {
        Student student = Fixtures.student();
        StudentParent link = new StudentParent();
        link.setParent(Fixtures.parent());
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(link));
        var school = Fixtures.school();
        school.setName("Very Long School Name");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        service.notifyParentsOfStudent(1L, student, "Absent", "Student absent", "ATTENDANCE", true);

        verify(notificationRepository, times(2)).save(any(InAppNotification.class));
        verify(smsGateway).send("+255700000000", "Very Long S", "Student absent");
    }

    @Test
    void skipsSmsWhenDisabledOrPhoneMissingAndSwallowsGatewayFailure() {
        Student student = Fixtures.student();
        student.setUser(null);
        StudentParent noPhone = new StudentParent();
        noPhone.setParent(Fixtures.parent());
        noPhone.getParent().getUser().setPhone(null);
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(noPhone));
        service.notifyParentsOfStudent(1L, student, "T", "B", "GENERAL", true);

        StudentParent failing = new StudentParent();
        failing.setParent(Fixtures.parent());
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(failing));
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("gateway")).when(smsGateway).send(any(), any(), any());
        service.notifyParentsOfStudent(1L, student, "T", "B", "GENERAL", true);
        service.notifyParentsOfStudent(1L, student, "T", "B", "GENERAL", false);
    }

    @Test
    void marksExistingNotificationReadAndIgnoresMissing() {
        InAppNotification notification = notification(1L, false);
        when(notificationRepository.findByIdAndUserId(1L, 4L)).thenReturn(Optional.of(notification));
        service.markRead(4L, 1L);
        assertThat(notification.isReadFlag()).isTrue();

        when(notificationRepository.findByIdAndUserId(2L, 4L)).thenReturn(Optional.empty());
        service.markRead(4L, 2L);
    }

    private InAppNotification notification(Long id, boolean read) {
        InAppNotification notification = new InAppNotification();
        notification.setId(id);
        notification.setTitle("Title");
        notification.setBody("Body");
        notification.setCategory("GENERAL");
        notification.setReadFlag(read);
        return notification;
    }
}
