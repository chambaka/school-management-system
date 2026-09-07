package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.communication.CreateStudentMessageRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.SmsDelivery;
import tz.co.chambaka.school.management.model.StudentCommunication;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.enums.RelationshipType;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.SmsDeliveryStatus;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentCommunicationRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.sms.SmsGateway;
import tz.co.chambaka.school.management.sms.SmsSendResult;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCommunicationServiceTest {

    @Mock
    private StudentCommunicationRepository communicationRepository;
    @Mock
    private StudentParentRepository studentParentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private ParentService parentService;
    @Mock
    private SmsGateway smsGateway;
    @Mock
    private SmsProperties smsProperties;
    @InjectMocks
    private StudentCommunicationService service;

    @Test
    void staffPostAndParentThread() {
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(userRepository.findById(10L)).thenReturn(Optional.of(Fixtures.user(10L, Role.TEACHER)));
        when(communicationRepository.save(any(StudentCommunication.class))).thenAnswer(inv -> {
            StudentCommunication saved = inv.getArgument(0);
            saved.setId(8L);
            saved.setCreatedAt(Instant.parse("2026-09-07T07:00:00Z"));
            return saved;
        });
        var posted = service.post(1L, 1L, Fixtures.principal(Role.TEACHER),
                new CreateStudentMessageRequest("Please collect report cards.", false));
        assertThat(posted.body()).contains("report cards");
        assertThat(posted.notifyParentsSms()).isFalse();
        verify(smsGateway, never()).send(any(), any(), any());

        when(communicationRepository.findBySchoolIdAndStudentIdOrderByCreatedAtAsc(1L, 1L))
                .thenReturn(List.of(message(Role.TEACHER, false)));
        assertThat(service.listForStudent(1L, 1L, Fixtures.principal(Role.ADMIN))).hasSize(1);
        service.listForStudent(1L, 1L, Fixtures.principal(Role.PARENT));
        verify(parentService).assertLinked(10L, 1L);

        when(parentService.requireByUser(10L)).thenReturn(Fixtures.parent());
        when(studentParentRepository.findByParentId(1L)).thenReturn(List.of());
        assertThat(service.inbox(1L, Fixtures.principal(Role.PARENT))).isEmpty();

        StudentParent link = new StudentParent();
        link.setStudent(Fixtures.student());
        link.setParent(Fixtures.parent());
        when(studentParentRepository.findByParentId(1L)).thenReturn(List.of(link));
        when(communicationRepository.findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(List.of(message(Role.ADMIN, true)));
        assertThat(service.inbox(1L, Fixtures.principal(Role.PARENT)).getFirst().smsSent()).isEqualTo(1);
    }

    @Test
    void adminSmsFlagAlertsLinkedParents() {
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(userRepository.findById(10L)).thenReturn(Optional.of(Fixtures.user(10L, Role.ADMIN)));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(smsProperties.messaging()).thenReturn(new SmsProperties.Messaging(
                true, "log-sms", "HALO", "http://localhost:7575", "secret"));
        when(smsGateway.send(any(), eq("HALO"), any())).thenReturn(SmsSendResult.ok("queued"));
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(
                link(Fixtures.parent()),
                link(parentWithoutPhone()),
                link(parentNamed("Fail", "0711111111"))
        ));
        when(smsGateway.send(eq("+255711111111"), any(), any())).thenReturn(SmsSendResult.failed("rejected"));
        when(communicationRepository.save(any(StudentCommunication.class))).thenAnswer(inv -> {
            StudentCommunication saved = inv.getArgument(0);
            saved.setId(3L);
            saved.setCreatedAt(Instant.parse("2026-09-07T07:00:00Z"));
            return saved;
        });
        var response = service.post(1L, 1L, Fixtures.principal(Role.ADMIN),
                new CreateStudentMessageRequest("Fee reminder for next week.", true));
        assertThat(response.notifyParentsSms()).isTrue();
        assertThat(response.smsSent()).isEqualTo(1);
        assertThat(response.smsFailed()).isEqualTo(1);
        assertThat(response.smsSkipped()).isEqualTo(1);
    }

    @Test
    void smsDisabledSkippedAndGatewaySkip() {
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(userRepository.findById(10L)).thenReturn(Optional.of(Fixtures.user(10L, Role.TENANT_ADMIN)));
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());
        when(smsProperties.messaging()).thenReturn(SmsProperties.Messaging.defaults());
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(link(Fixtures.parent())));
        when(communicationRepository.save(any(StudentCommunication.class))).thenAnswer(inv -> inv.getArgument(0));
        var disabled = service.post(1L, 1L, Fixtures.principal(Role.TENANT_ADMIN),
                new CreateStudentMessageRequest("Note", true));
        assertThat(disabled.smsSkipped()).isEqualTo(1);

        when(smsProperties.messaging()).thenReturn(new SmsProperties.Messaging(
                true, "log-sms", "HALO", "http://localhost:7575", "secret"));
        when(smsGateway.send(any(), any(), any())).thenReturn(SmsSendResult.skipped("rate-limit"));
        var skipped = service.post(1L, 1L, Fixtures.principal(Role.TENANT_ADMIN),
                new CreateStudentMessageRequest("Note two", true));
        assertThat(skipped.smsSkipped()).isEqualTo(1);
    }

    @Test
    void guardsAndParentReplyClearsSmsFlag() {
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        assertThatThrownBy(() -> service.post(1L, 1L, Fixtures.principal(Role.STUDENT),
                new CreateStudentMessageRequest("x", false))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.post(1L, 1L, Fixtures.principal(Role.TEACHER),
                new CreateStudentMessageRequest("x", true))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.post(1L, 1L, Fixtures.principal(Role.ADMIN),
                new CreateStudentMessageRequest("x", false))).isInstanceOf(ResourceNotFoundException.class);

        when(userRepository.findById(10L)).thenReturn(Optional.of(Fixtures.user(10L, Role.PARENT)));
        when(communicationRepository.save(any(StudentCommunication.class))).thenAnswer(inv -> inv.getArgument(0));
        var reply = service.post(1L, 1L, Fixtures.principal(Role.PARENT),
                new CreateStudentMessageRequest("We will come tomorrow.", true));
        assertThat(reply.notifyParentsSms()).isFalse();
        verify(parentService).assertLinked(10L, 1L);

        assertThatThrownBy(() -> service.listForStudent(1L, 1L, Fixtures.principal(Role.STUDENT)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.inbox(1L, Fixtures.principal(Role.ADMIN)))
                .isInstanceOf(ApiException.class);
        assertThat(StudentCommunicationService.composeSms("S", "Ada", null)).contains("Reply in the app.");
        assertThat(StudentCommunicationService.composeSms("S", "Ada", "word ".repeat(40)).length()).isLessThanOrEqualTo(160);
        assertThat(StudentCommunicationService.composeSms("Very Long School Name For Truncation Path",
                "Very Long Student Name", "x".repeat(120)).length()).isLessThanOrEqualTo(160);
    }

    private StudentCommunication message(Role role, boolean withDelivery) {
        StudentCommunication message = new StudentCommunication();
        message.setId(2L);
        message.setStudent(Fixtures.student());
        message.setAuthor(Fixtures.user(10L, role));
        message.setAuthorRole(role);
        message.setBody("Hello");
        message.setNotifyParentsSms(withDelivery);
        message.setCreatedAt(Instant.parse("2026-09-07T07:00:00Z"));
        if (withDelivery) {
            SmsDelivery sent = new SmsDelivery();
            sent.setParent(Fixtures.parent());
            sent.setPhone("255700000000");
            sent.setStatus(SmsDeliveryStatus.SENT);
            SmsDelivery failed = new SmsDelivery();
            failed.setParent(Fixtures.parent());
            failed.setStatus(SmsDeliveryStatus.FAILED);
            failed.setError("down");
            SmsDelivery skipped = new SmsDelivery();
            skipped.setParent(Fixtures.parent());
            skipped.setStatus(SmsDeliveryStatus.SKIPPED);
            skipped.setError("no-phone");
            message.getSmsDeliveries().add(sent);
            message.getSmsDeliveries().add(failed);
            message.getSmsDeliveries().add(skipped);
        }
        return message;
    }

    private StudentParent link(Parent parent) {
        StudentParent link = new StudentParent();
        link.setStudent(Fixtures.student());
        link.setParent(parent);
        link.setRelationship(RelationshipType.MOTHER);
        return link;
    }

    private Parent parentWithoutPhone() {
        Parent parent = Fixtures.parent();
        parent.getUser().setPhone(null);
        return parent;
    }

    private Parent parentNamed(String name, String phone) {
        Parent parent = Fixtures.parent();
        parent.setId(9L);
        parent.getUser().setName(name);
        parent.getUser().setPhone(phone);
        return parent;
    }
}
