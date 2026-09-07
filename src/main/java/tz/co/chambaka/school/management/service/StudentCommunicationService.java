package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.communication.CreateStudentMessageRequest;
import tz.co.chambaka.school.management.dto.communication.SmsDeliveryResponse;
import tz.co.chambaka.school.management.dto.communication.StudentMessageResponse;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.SmsDelivery;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentCommunication;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.SmsDeliveryStatus;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentCommunicationRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import tz.co.chambaka.school.management.sms.SmsGateway;
import tz.co.chambaka.school.management.sms.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentCommunicationService {

    private static final Logger log = LoggerFactory.getLogger(StudentCommunicationService.class);

    private final StudentCommunicationRepository communicationRepository;
    private final StudentParentRepository studentParentRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final StudentService studentService;
    private final ParentService parentService;
    private final SmsGateway smsGateway;
    private final SmsProperties smsProperties;

    public StudentCommunicationService(
            StudentCommunicationRepository communicationRepository,
            StudentParentRepository studentParentRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            StudentService studentService,
            ParentService parentService,
            SmsGateway smsGateway,
            SmsProperties smsProperties
    ) {
        this.communicationRepository = communicationRepository;
        this.studentParentRepository = studentParentRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.studentService = studentService;
        this.parentService = parentService;
        this.smsGateway = smsGateway;
        this.smsProperties = smsProperties;
    }

    @Transactional(readOnly = true)
    public List<StudentMessageResponse> listForStudent(Long schoolId, Long studentId, UserPrincipal principal) {
        studentService.require(schoolId, studentId);
        requireCanView(principal, studentId);
        return communicationRepository.findBySchoolIdAndStudentIdOrderByCreatedAtAsc(schoolId, studentId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentMessageResponse> inbox(Long schoolId, UserPrincipal principal) {
        if (principal.getRole() != Role.PARENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only parents have a message inbox");
        }
        Parent parent = parentService.requireByUser(principal.getId());
        List<Long> studentIds = studentParentRepository.findByParentId(parent.getId()).stream()
                .map(link -> link.getStudent().getId())
                .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return communicationRepository.findBySchoolIdAndStudentIdInOrderByCreatedAtDesc(schoolId, studentIds)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public StudentMessageResponse post(Long schoolId, Long studentId, UserPrincipal principal, CreateStudentMessageRequest request) {
        Student student = studentService.require(schoolId, studentId);
        Role role = principal.getRole();
        boolean staff = role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.TEACHER;
        boolean parent = role == Role.PARENT;
        if (!staff && !parent) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to post student messages");
        }
        if (parent) {
            parentService.assertLinked(principal.getId(), studentId);
        }
        boolean notifySms = request.notifyParentsSms();
        if (parent) {
            notifySms = false;
        } else if (notifySms && role != Role.ADMIN && role != Role.TENANT_ADMIN) {
            throw new BusinessException("Only school admins can send parent SMS alerts");
        }
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getId()));
        StudentCommunication message = new StudentCommunication();
        message.setSchoolId(schoolId);
        message.setStudent(student);
        message.setAuthor(author);
        message.setAuthorRole(role);
        message.setBody(request.body().trim());
        message.setNotifyParentsSms(notifySms);
        if (notifySms) {
            alertParents(schoolId, student, message);
        }
        StudentCommunication saved = communicationRepository.save(message);
        log.info("Posted student message id={} studentId={} authorRole={} notifySms={}",
                saved.getId(), studentId, role, notifySms);
        return toResponse(saved);
    }

    private void alertParents(Long schoolId, Student student, StudentCommunication message) {
        List<StudentParent> links = studentParentRepository.findByStudentId(student.getId());
        String schoolName = schoolRepository.findById(schoolId).map(School::getName).orElse("School");
        String smsBody = composeSms(schoolName, student.getUser().getName(), message.getBody());
        SmsProperties.Messaging messaging = smsProperties.messaging();
        for (StudentParent link : links) {
            Parent parent = link.getParent();
            SmsDelivery delivery = new SmsDelivery();
            delivery.setSchoolId(schoolId);
            delivery.setCommunication(message);
            delivery.setParent(parent);
            delivery.setBody(smsBody);
            String phone = PhoneNumbers.toE164Like(parent.getUser().getPhone());
            delivery.setPhone(phone);
            if (phone == null) {
                delivery.setStatus(SmsDeliveryStatus.SKIPPED);
                delivery.setError("no-phone");
            } else if (!messaging.active()) {
                delivery.setStatus(SmsDeliveryStatus.SKIPPED);
                delivery.setError("sms-disabled");
            } else {
                SmsSendResult result = smsGateway.send(phone, messaging.senderId(), smsBody);
                if (result.sent()) {
                    delivery.setStatus(SmsDeliveryStatus.SENT);
                    delivery.setProviderRef(result.providerRef());
                } else if ("skipped".equals(result.providerRef())) {
                    delivery.setStatus(SmsDeliveryStatus.SKIPPED);
                    delivery.setError(result.error());
                } else {
                    delivery.setStatus(SmsDeliveryStatus.FAILED);
                    delivery.setError(result.error());
                }
            }
            message.getSmsDeliveries().add(delivery);
        }
    }

    static String composeSms(String schoolName, String studentName, String body) {
        String preview = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (preview.length() > 80) {
            preview = preview.substring(0, 77) + "...";
        }
        String text = schoolName + " / " + studentName + ": " + preview + " Reply in the app.";
        if (text.length() <= 160) {
            return text;
        }
        return text.substring(0, 157) + "...";
    }

    private void requireCanView(UserPrincipal principal, Long studentId) {
        Role role = principal.getRole();
        if (role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.TEACHER) {
            return;
        }
        if (role == Role.PARENT) {
            parentService.assertLinked(principal.getId(), studentId);
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view student messages");
    }

    private StudentMessageResponse toResponse(StudentCommunication message) {
        List<SmsDelivery> deliveries = message.getSmsDeliveries();
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (SmsDelivery delivery : deliveries) {
            switch (delivery.getStatus()) {
                case SENT -> sent++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
            }
        }
        List<SmsDeliveryResponse> deliveryResponses = deliveries.stream()
                .map(delivery -> new SmsDeliveryResponse(
                        delivery.getParent().getId(),
                        delivery.getParent().getUser().getName(),
                        PhoneNumbers.mask(delivery.getPhone()),
                        delivery.getStatus(),
                        delivery.getError()))
                .toList();
        return new StudentMessageResponse(
                message.getId(),
                message.getStudent().getId(),
                message.getStudent().getUser().getName(),
                message.getAuthor().getId(),
                message.getAuthor().getName(),
                message.getAuthorRole(),
                message.getBody(),
                message.isNotifyParentsSms(),
                sent,
                failed,
                skipped,
                message.getCreatedAt(),
                deliveryResponses
        );
    }
}
