package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.FeeStructureResponse;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.InvoiceItemResponse;
import tz.co.chambaka.school.management.dto.finance.InvoiceResponse;
import tz.co.chambaka.school.management.dto.finance.PaymentResponse;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.FeeStructure;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.InvoiceItem;
import tz.co.chambaka.school.management.model.Payment;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.FeeStructureRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.PaymentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private static final Logger log = LoggerFactory.getLogger(FinanceService.class);

    private final FeeStructureRepository feeStructureRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicYearService academicYearService;
    private final ClassService classService;
    private final StudentService studentService;
    private final ParentService parentService;
    private final AuditService auditService;

    public FinanceService(
            FeeStructureRepository feeStructureRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicYearService academicYearService,
            ClassService classService,
            StudentService studentService,
            ParentService parentService,
            AuditService auditService
    ) {
        this.feeStructureRepository = feeStructureRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.academicYearService = academicYearService;
        this.classService = classService;
        this.studentService = studentService;
        this.parentService = parentService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listFees(Long schoolId, Long academicYearId) {
        List<FeeStructureResponse> fees = feeStructureRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId)
                .stream().map(this::toFee).toList();
        audit(schoolId, AuditAction.FEE_LISTED, "FeeStructure", academicYearId,
                "Listed " + fees.size() + " fee structures for academic year " + academicYearId,
                "academicYearId=" + academicYearId + " count=" + fees.size()
                        + " feeIds=" + fees.stream().map(f -> String.valueOf(f.id())).collect(Collectors.joining(",")));
        return fees;
    }

    @Transactional
    public FeeStructureResponse createFee(Long schoolId, FeeStructureRequest request) {
        FeeStructure fee = new FeeStructure();
        fee.setSchoolId(schoolId);
        fee.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        fee.setSchoolClass(request.schoolClassId() == null ? null : classService.require(schoolId, request.schoolClassId()));
        fee.setName(request.name());
        fee.setFeeType(request.feeType());
        fee.setFrequency(request.frequency());
        fee.setAmount(request.amount());
        fee.setDueDate(request.dueDate());
        FeeStructure saved = feeStructureRepository.save(fee);
        log.info("Created fee structure id={} schoolId={} name={}", saved.getId(), schoolId, saved.getName());
        audit(schoolId, AuditAction.FEE_CREATED, "FeeStructure", saved.getId(),
                "Created fee " + saved.getName() + " amount=" + saved.getAmount(),
                "name=" + saved.getName()
                        + " amount=" + saved.getAmount()
                        + " type=" + saved.getFeeType()
                        + " frequency=" + saved.getFrequency()
                        + " academicYearId=" + request.academicYearId()
                        + " classId=" + request.schoolClassId()
                        + " dueDate=" + saved.getDueDate());
        return toFee(saved);
    }

    @Transactional
    public List<InvoiceResponse> generateInvoices(Long schoolId, GenerateInvoicesRequest request) {
        AcademicYear year = academicYearService.require(schoolId, request.academicYearId());
        classService.require(schoolId, request.schoolClassId());
        List<FeeStructure> fees = request.feeStructureIds().stream()
                .map(id -> feeStructureRepository.findByIdAndSchoolId(id, schoolId)
                        .orElseThrow(() -> ResourceNotFoundException.of("FeeStructure", id)))
                .toList();
        List<Student> students = studentRepository.findBySchoolIdAndSchoolClassId(schoolId, request.schoolClassId());
        if (students.isEmpty()) {
            throw new BusinessException("No students found in this class");
        }
        String feeNames = fees.stream().map(FeeStructure::getName).collect(Collectors.joining(","));
        List<InvoiceResponse> created = new ArrayList<>();
        for (Student student : students) {
            Invoice invoice = new Invoice();
            invoice.setSchoolId(schoolId);
            invoice.setStudent(student);
            invoice.setAcademicYear(year);
            invoice.setInvoiceNumber(nextInvoiceNumber(schoolId));
            invoice.setIssuedAt(Instant.now());
            invoice.setDueDate(request.dueDate() != null ? request.dueDate() : LocalDate.now().plusDays(14));
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setStatus(InvoiceStatus.PENDING);
            BigDecimal total = BigDecimal.ZERO;
            for (FeeStructure fee : fees) {
                InvoiceItem item = new InvoiceItem();
                item.setSchoolId(schoolId);
                item.setInvoice(invoice);
                item.setFeeStructure(fee);
                item.setDescription(fee.getName());
                item.setAmount(fee.getAmount());
                invoice.getItems().add(item);
                total = total.add(fee.getAmount());
            }
            invoice.setTotalAmount(total);
            Invoice saved = invoiceRepository.save(invoice);
            created.add(toInvoice(saved));
            audit(schoolId, AuditAction.INVOICE_GENERATED, "Invoice", saved.getId(),
                    "Generated invoice " + saved.getInvoiceNumber() + " for student " + student.getId()
                            + " total=" + saved.getTotalAmount(),
                    "invoiceNumber=" + saved.getInvoiceNumber()
                            + " studentId=" + student.getId()
                            + " admissionNo=" + student.getAdmissionNo()
                            + " studentName=" + student.getUser().getName()
                            + " total=" + saved.getTotalAmount()
                            + " dueDate=" + saved.getDueDate()
                            + " academicYearId=" + year.getId()
                            + " classId=" + request.schoolClassId()
                            + " feeIds=" + request.feeStructureIds()
                            + " feeNames=" + feeNames);
        }
        log.info("Generated {} invoices schoolId={} classId={} yearId={}",
                created.size(), schoolId, request.schoolClassId(), request.academicYearId());
        return created;
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> listInvoices(Long schoolId, Pageable pageable) {
        PageResponse<InvoiceResponse> page = PageResponse.of(
                invoiceRepository.findBySchoolId(schoolId, pageable).map(this::toInvoice));
        audit(schoolId, AuditAction.INVOICE_LISTED, "Invoice", null,
                "Listed invoices page=" + pageable.getPageNumber() + " count=" + page.content().size(),
                "page=" + pageable.getPageNumber()
                        + " size=" + pageable.getPageSize()
                        + " count=" + page.content().size()
                        + " invoiceNumbers=" + page.content().stream()
                        .map(InvoiceResponse::invoiceNumber)
                        .collect(Collectors.joining(",")));
        return page;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> studentInvoices(Long schoolId, Long studentId, UserPrincipal principal) {
        studentService.require(schoolId, studentId);
        requireStudentFinanceAccess(principal, studentId);
        List<InvoiceResponse> invoices = invoiceRepository.findBySchoolIdAndStudentId(schoolId, studentId)
                .stream().map(this::toInvoice).toList();
        audit(schoolId, AuditAction.INVOICE_LISTED, "Student", studentId,
                "Listed " + invoices.size() + " invoices for student " + studentId,
                "studentId=" + studentId
                        + " count=" + invoices.size()
                        + " invoiceNumbers=" + invoices.stream()
                        .map(InvoiceResponse::invoiceNumber)
                        .collect(Collectors.joining(","))
                        + " outstandingSum=" + invoices.stream()
                        .map(InvoiceResponse::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        return invoices;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long schoolId, Long id, UserPrincipal principal) {
        Invoice invoice = requireInvoice(schoolId, id);
        requireStudentFinanceAccess(principal, invoice.getStudent().getId());
        InvoiceResponse response = toInvoice(invoice);
        audit(schoolId, AuditAction.INVOICE_VIEWED, "Invoice", invoice.getId(),
                "Viewed invoice " + invoice.getInvoiceNumber() + " student=" + invoice.getStudent().getId()
                        + " status=" + invoice.getStatus(),
                invoiceDetails(invoice));
        return response;
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingBalance(Long schoolId, Long studentId, UserPrincipal principal) {
        studentService.require(schoolId, studentId);
        requireStudentFinanceAccess(principal, studentId);
        BigDecimal outstanding = invoiceRepository.findBySchoolIdAndStudentIdAndStatusIn(
                        schoolId, studentId, List.copyOf(EnumSet.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE)))
                .stream()
                .map(Invoice::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        audit(schoolId, AuditAction.BALANCE_VIEWED, "Student", studentId,
                "Viewed outstanding balance for student " + studentId + " amount=" + outstanding,
                "studentId=" + studentId + " outstanding=" + outstanding);
        return outstanding;
    }

    @Transactional
    public PaymentResponse recordPayment(Long schoolId, RecordPaymentRequest request, Long recordedBy) {
        Invoice invoice = requireInvoice(schoolId, request.invoiceId());
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Cannot pay a cancelled invoice");
        }
        if (request.amount().compareTo(invoice.getBalance()) > 0) {
            throw new BusinessException("Payment exceeds outstanding balance");
        }
        InvoiceStatus previousStatus = invoice.getStatus();
        User recorder = userRepository.findById(recordedBy).orElse(null);
        Payment payment = new Payment();
        payment.setSchoolId(schoolId);
        payment.setInvoice(invoice);
        payment.setStudent(invoice.getStudent());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setTransactionRef(request.transactionRef());
        payment.setReceiptNumber(nextReceiptNumber(schoolId));
        payment.setPaidAt(Instant.now());
        payment.setRecordedBy(recorder);
        payment = paymentRepository.save(payment);

        invoice.setPaidAmount(invoice.getPaidAmount().add(request.amount()));
        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }
        log.info("Recorded payment id={} invoiceId={} amount={} status={}",
                payment.getId(), invoice.getId(), payment.getAmount(), invoice.getStatus());
        audit(schoolId, AuditAction.PAYMENT_RECORDED, "Payment", payment.getId(),
                "Recorded payment " + payment.getReceiptNumber() + " amount=" + payment.getAmount()
                        + " on " + invoice.getInvoiceNumber() + " " + previousStatus + "->" + invoice.getStatus(),
                "receiptNumber=" + payment.getReceiptNumber()
                        + " invoiceId=" + invoice.getId()
                        + " invoiceNumber=" + invoice.getInvoiceNumber()
                        + " studentId=" + invoice.getStudent().getId()
                        + " amount=" + payment.getAmount()
                        + " method=" + payment.getMethod()
                        + " transactionRef=" + payment.getTransactionRef()
                        + " recordedBy=" + recordedBy
                        + " previousStatus=" + previousStatus
                        + " newStatus=" + invoice.getStatus()
                        + " invoiceTotal=" + invoice.getTotalAmount()
                        + " paidAmount=" + invoice.getPaidAmount()
                        + " remaining=" + invoice.getBalance());
        return toPayment(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> paymentsForInvoice(Long schoolId, Long invoiceId, UserPrincipal principal) {
        Invoice invoice = requireInvoice(schoolId, invoiceId);
        requireStudentFinanceAccess(principal, invoice.getStudent().getId());
        List<PaymentResponse> payments = paymentRepository.findBySchoolIdAndInvoiceId(schoolId, invoiceId)
                .stream().map(this::toPayment).toList();
        audit(schoolId, AuditAction.PAYMENT_LISTED, "Invoice", invoiceId,
                "Listed " + payments.size() + " payments for invoice " + invoice.getInvoiceNumber(),
                "invoiceId=" + invoiceId
                        + " invoiceNumber=" + invoice.getInvoiceNumber()
                        + " studentId=" + invoice.getStudent().getId()
                        + " count=" + payments.size()
                        + " receiptNumbers=" + payments.stream()
                        .map(PaymentResponse::receiptNumber)
                        .collect(Collectors.joining(",")));
        return payments;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long schoolId, Long id, UserPrincipal principal) {
        Payment payment = paymentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", id));
        requireStudentFinanceAccess(principal, payment.getStudent().getId());
        PaymentResponse response = toPayment(payment);
        audit(schoolId, AuditAction.PAYMENT_VIEWED, "Payment", payment.getId(),
                "Viewed payment " + payment.getReceiptNumber() + " amount=" + payment.getAmount(),
                "receiptNumber=" + payment.getReceiptNumber()
                        + " paymentId=" + payment.getId()
                        + " invoiceId=" + payment.getInvoice().getId()
                        + " invoiceNumber=" + payment.getInvoice().getInvoiceNumber()
                        + " studentId=" + payment.getStudent().getId()
                        + " amount=" + payment.getAmount()
                        + " method=" + payment.getMethod()
                        + " transactionRef=" + payment.getTransactionRef());
        return response;
    }

    void requireStudentFinanceAccess(UserPrincipal principal, Long studentId) {
        if (principal == null || principal.getRole() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this financial record");
        }
        Role role = principal.getRole();
        if (role == Role.ADMIN || role == Role.TENANT_ADMIN) {
            return;
        }
        if (role == Role.STUDENT) {
            Long mine = studentService.requireByUser(principal.getId()).getId();
            if (!mine.equals(studentId)) {
                throw new ResourceNotFoundException("Invoice not found for current student");
            }
            return;
        }
        if (role == Role.PARENT) {
            parentService.assertLinked(principal.getId(), studentId);
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this financial record");
    }

    private void audit(
            Long schoolId,
            AuditAction action,
            String resourceType,
            Object resourceId,
            String summary,
            String details
    ) {
        auditService.recordFinance(
                schoolId,
                action,
                resourceType,
                resourceId == null ? null : String.valueOf(resourceId),
                summary,
                details);
    }

    private static String invoiceDetails(Invoice invoice) {
        return "invoiceNumber=" + invoice.getInvoiceNumber()
                + " studentId=" + invoice.getStudent().getId()
                + " admissionNo=" + invoice.getStudent().getAdmissionNo()
                + " total=" + invoice.getTotalAmount()
                + " paid=" + invoice.getPaidAmount()
                + " balance=" + invoice.getBalance()
                + " status=" + invoice.getStatus()
                + " dueDate=" + invoice.getDueDate();
    }

    private Invoice requireInvoice(Long schoolId, Long id) {
        return invoiceRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", id));
    }

    private String nextInvoiceNumber(Long schoolId) {
        return "INV-" + schoolId + "-" + String.format("%06d", invoiceRepository.countBySchoolId(schoolId) + 1);
    }

    private String nextReceiptNumber(Long schoolId) {
        return "RCPT-" + schoolId + "-" + String.format("%06d", paymentRepository.countBySchoolId(schoolId) + 1);
    }

    private FeeStructureResponse toFee(FeeStructure fee) {
        SchoolClass schoolClass = fee.getSchoolClass();
        return new FeeStructureResponse(
                fee.getId(),
                fee.getAcademicYear().getId(),
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                fee.getName(),
                fee.getFeeType(),
                fee.getFrequency(),
                fee.getAmount(),
                fee.getDueDate()
        );
    }

    private InvoiceResponse toInvoice(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getStudent().getId(),
                invoice.getStudent().getUser().getName(),
                invoice.getStudent().getAdmissionNo(),
                invoice.getInvoiceNumber(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getBalance(),
                invoice.getStatus(),
                invoice.getDueDate(),
                invoice.getIssuedAt(),
                invoice.getItems().stream()
                        .map(item -> new InvoiceItemResponse(item.getId(), item.getDescription(), item.getAmount()))
                        .toList()
        );
    }

    private PaymentResponse toPayment(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoice().getId(),
                payment.getInvoice().getInvoiceNumber(),
                payment.getStudent().getId(),
                payment.getStudent().getUser().getName(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getTransactionRef(),
                payment.getReceiptNumber(),
                payment.getPaidAt()
        );
    }
}
