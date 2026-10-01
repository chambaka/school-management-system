package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.notification.NotificationChannel;
import tz.co.chambaka.school.management.notification.NotificationSettingsService;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.model.InAppNotification;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InAppNotificationRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.sms.SmsGateway;
import tz.co.chambaka.school.management.support.Fixtures;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock InAppNotificationRepository notificationRepository;
    @Mock StudentParentRepository studentParentRepository;
    @Mock SchoolRepository schoolRepository;
    @Mock AssignmentRepository assignmentRepository;
    @Mock ExamRepository examRepository;
    @Mock StudentRepository studentRepository;
    @Mock SmsGateway smsGateway;
    @Mock SmsProperties smsProperties;
    @Mock NotificationSettingsService notificationSettings;
    @Mock UserRepository userRepository;
    @InjectMocks AlertService service;

    @BeforeEach
    void noChannelChoice() {
        lenient().when(notificationSettings.preferredChannel(any())).thenReturn(Optional.empty());
    }

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
    void sendsOnlyTheChannelTheUserChose() {
        Student student = Fixtures.student();
        student.setUser(null);
        StudentParent link = new StudentParent();
        link.setParent(Fixtures.parent());
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(link));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(notificationSettings.preferredChannel(5L)).thenReturn(Optional.of(NotificationChannel.SMS));

        service.notifyParentsOfStudent(1L, student, "Absent", "Student absent", "ATTENDANCE", true);

        verify(notificationRepository, never()).save(any());
        verify(smsGateway).send("+255700000000", "Chambaka Se", "Student absent");

        when(notificationSettings.preferredChannel(5L)).thenReturn(Optional.of(NotificationChannel.IN_APP));
        service.notifyParentsOfStudent(1L, student, "Absent again", "Still absent", "ATTENDANCE", true);
        verify(notificationRepository).save(any(InAppNotification.class));
        verify(smsGateway, times(1)).send(any(), any(), any());
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
    void storesEntityOnParentAlertAndDropsAlertsForMissingEntities() {
        Student student = Fixtures.student();
        StudentParent link = new StudentParent();
        link.setParent(Fixtures.parent());
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(link));
        service.notifyParentsOfStudent(1L, student, "New assignment", "Algebra is due 2026-09-20",
                "ASSIGNMENT", false, AlertService.SUBJECT_ASSIGNMENT, 8L);
        verify(notificationRepository, times(2)).save(argThat(saved ->
                AlertService.SUBJECT_ASSIGNMENT.equals(saved.getEntityType()) && Long.valueOf(8L).equals(saved.getEntityId())));

        InAppNotification live = notification(1L, false);
        live.setSchoolId(1L);
        live.setEntityType(AlertService.SUBJECT_ASSIGNMENT);
        live.setEntityId(8L);
        InAppNotification stale = notification(2L, false);
        stale.setSchoolId(1L);
        stale.setEntityType(AlertService.SUBJECT_ASSIGNMENT);
        stale.setEntityId(9L);
        InAppNotification examAlert = notification(3L, false);
        examAlert.setSchoolId(1L);
        examAlert.setEntityType(AlertService.SUBJECT_EXAM);
        examAlert.setEntityId(4L);
        InAppNotification studentAlert = notification(4L, false);
        studentAlert.setSchoolId(1L);
        studentAlert.setEntityType(AlertService.SUBJECT_STUDENT);
        studentAlert.setEntityId(1L);
        InAppNotification unknown = notification(5L, false);
        unknown.setSchoolId(1L);
        unknown.setEntityType("NOTICE");
        unknown.setEntityId(1L);
        when(notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(1L, 4L))
                .thenReturn(List.of(live, stale, examAlert, studentAlert, unknown));
        when(assignmentRepository.findByIdAndSchoolId(8L, 1L)).thenReturn(Optional.of(new tz.co.chambaka.school.management.model.Assignment()));
        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        when(examRepository.findByIdAndSchoolId(4L, 1L)).thenReturn(Optional.empty());
        when(studentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(Fixtures.student()));

        assertThat(service.inbox(1L, 4L)).extracting(row -> row.id()).containsExactly(1L, 4L, 5L);
        verify(notificationRepository).deleteAll(List.of(stale, examAlert));
    }

    @Test
    void notifiesEachPersonOnceAndRefreshesUnreadInsteadOfStacking() {
        Student student = Fixtures.student();
        Student classmate = Fixtures.student();
        classmate.setId(2L);
        classmate.setUser(Fixtures.user(6L, Role.STUDENT));
        StudentParent first = new StudentParent();
        first.setParent(Fixtures.parent());
        StudentParent duplicate = new StudentParent();
        duplicate.setParent(Fixtures.parent());
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(first, duplicate));
        when(studentParentRepository.findByStudentId(2L)).thenReturn(List.of(duplicate));

        service.notifyParentsOfStudent(1L, student, "Assignment submitted", "Done",
                "ASSIGNMENT", false, AlertService.SUBJECT_ASSIGNMENT, 8L, false);
        verify(notificationRepository, times(1)).save(any(InAppNotification.class));

        service.notifyHouseholds(1L, List.of(student, classmate), "New assignment", "Due tomorrow",
                "ASSIGNMENT", false, AlertService.SUBJECT_ASSIGNMENT, 8L, true);
        verify(notificationRepository, times(4)).save(any(InAppNotification.class));

        service.notifyUser(1L, 4L, "Assignment submitted", "Latest attempt", "ASSIGNMENT",
                AlertService.SUBJECT_ASSIGNMENT, 8L);
        verify(notificationRepository, times(5)).save(any(InAppNotification.class));

        service.notifyUser(1L, null, "Ignored", "Body", "ASSIGNMENT", AlertService.SUBJECT_ASSIGNMENT, 8L);
        service.notifyHouseholds(1L, List.of(), "New assignment", "Due", "ASSIGNMENT", false,
                AlertService.SUBJECT_ASSIGNMENT, 8L, true);
        service.notifyHouseholds(1L, java.util.Arrays.asList(student, null), "New assignment", "Due", "ASSIGNMENT", false,
                AlertService.SUBJECT_ASSIGNMENT, 8L, true);
    }

    @Test
    void inboxKeepsEachSubmissionAlert() {
        InAppNotification newest = notification(10L, false);
        newest.setSchoolId(1L);
        newest.setEntityType(AlertService.SUBJECT_ASSIGNMENT);
        newest.setEntityId(8L);
        newest.setTitle("Assignment submitted");
        InAppNotification older = notification(11L, false);
        older.setSchoolId(1L);
        older.setEntityType(AlertService.SUBJECT_ASSIGNMENT);
        older.setEntityId(8L);
        older.setTitle("Assignment submitted");
        InAppNotification read = notification(12L, true);
        read.setSchoolId(1L);
        read.setEntityType(AlertService.SUBJECT_ASSIGNMENT);
        read.setEntityId(8L);
        read.setTitle("Assignment submitted");
        when(notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(1L, 4L))
                .thenReturn(List.of(newest, older, read));
        when(assignmentRepository.findByIdAndSchoolId(8L, 1L)).thenReturn(Optional.of(new tz.co.chambaka.school.management.model.Assignment()));

        assertThat(service.inbox(1L, 4L)).extracting(row -> row.id()).containsExactly(10L, 11L, 12L);
    }

    @Test
    void inboxCollapsesIdenticalUnreadCopiesThatAreNotLinkedToAnAssignment() {
        InAppNotification newest = notification(21L, false);
        newest.setTitle("New assignment");
        newest.setBody("Algebra is due 2026-10-01");
        InAppNotification older = notification(22L, false);
        older.setTitle("New assignment");
        older.setBody("Algebra is due 2026-10-01");
        InAppNotification other = notification(23L, false);
        other.setTitle("New assignment");
        other.setBody("Essay is due 2026-10-03");
        newest.setSchoolId(1L);
        older.setSchoolId(1L);
        other.setSchoolId(1L);
        tz.co.chambaka.school.management.model.Assignment algebra = new tz.co.chambaka.school.management.model.Assignment();
        algebra.setTitle("Algebra");
        tz.co.chambaka.school.management.model.Assignment essay = new tz.co.chambaka.school.management.model.Assignment();
        essay.setTitle("Essay");
        when(notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(1L, 4L))
                .thenReturn(List.of(newest, older, other));
        when(assignmentRepository.findBySchoolIdOrderByDueDateDesc(1L)).thenReturn(List.of(algebra, essay));

        assertThat(service.inbox(1L, 4L)).extracting(row -> row.id()).containsExactly(21L, 23L);
        verify(notificationRepository).deleteAll(List.of(older));
    }

    @Test
    void inboxDropsAlertsWhenTheAssignmentIsNoLongerListed() {
        InAppNotification gone = notification(31L, false);
        gone.setSchoolId(1L);
        gone.setTitle("New assignment");
        gone.setBody("Pure Maths is due 2026-10-02");
        InAppNotification live = notification(32L, false);
        live.setSchoolId(1L);
        live.setTitle("New assignment");
        live.setBody("Calculas is due 2026-10-09");
        tz.co.chambaka.school.management.model.Assignment calculas = new tz.co.chambaka.school.management.model.Assignment();
        calculas.setTitle("Calculas");
        when(notificationRepository.findBySchoolIdAndUserIdOrderByCreatedAtDesc(1L, 4L))
                .thenReturn(List.of(gone, live));
        when(assignmentRepository.findBySchoolIdOrderByDueDateDesc(1L)).thenReturn(List.of(calculas));

        assertThat(service.inbox(1L, 4L)).extracting(row -> row.id()).containsExactly(32L);
        verify(notificationRepository).deleteAll(List.of(gone));
    }

    @Test
    void removeForEntityDeletesLinkedAlertsAndIgnoresBlankSubject() {
        service.removeForEntity(1L, AlertService.SUBJECT_ASSIGNMENT, 8L);
        verify(notificationRepository).deleteBySchoolIdAndEntityTypeAndEntityId(1L, AlertService.SUBJECT_ASSIGNMENT, 8L);
        service.removeForEntity(1L, "  ", 8L);
        service.removeForEntity(1L, null, 8L);
        service.removeForEntity(1L, AlertService.SUBJECT_ASSIGNMENT, null);
        verify(notificationRepository, times(1)).deleteBySchoolIdAndEntityTypeAndEntityId(any(), any(), any());
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
