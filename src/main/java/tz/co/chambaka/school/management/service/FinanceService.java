package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.FeeStructureResponse;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.InvoiceItemResponse;
import tz.co.chambaka.school.management.dto.finance.InvoiceResponse;
import tz.co.chambaka.school.management.dto.finance.PaymentResponse;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
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
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.repository.FeeStructureRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.PaymentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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

    public FinanceService(
            FeeStructureRepository feeStructureRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicYearService academicYearService,
            ClassService classService,
            StudentService studentService
    ) {
        this.feeStructureRepository = feeStructureRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.academicYearService = academicYearService;
        this.classService = classService;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> listFees(Long schoolId, Long academicYearId) {
        return feeStructureRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId)
                .stream().map(this::toFee).toList();
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
            created.add(toInvoice(invoiceRepository.save(invoice)));
        }
        log.info("Generated {} invoices schoolId={} classId={} yearId={}",
                created.size(), schoolId, request.schoolClassId(), request.academicYearId());
        return created;
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> listInvoices(Long schoolId, Pageable pageable) {
        return PageResponse.of(invoiceRepository.findBySchoolId(schoolId, pageable).map(this::toInvoice));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> studentInvoices(Long schoolId, Long studentId) {
        studentService.require(schoolId, studentId);
        return invoiceRepository.findBySchoolIdAndStudentId(schoolId, studentId).stream().map(this::toInvoice).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long schoolId, Long id) {
        return toInvoice(requireInvoice(schoolId, id));
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingBalance(Long schoolId, Long studentId) {
        studentService.require(schoolId, studentId);
        return invoiceRepository.findBySchoolIdAndStudentIdAndStatusIn(
                        schoolId, studentId, List.copyOf(EnumSet.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE)))
                .stream()
                .map(Invoice::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        return toPayment(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> paymentsForInvoice(Long schoolId, Long invoiceId) {
        requireInvoice(schoolId, invoiceId);
        return paymentRepository.findBySchoolIdAndInvoiceId(schoolId, invoiceId).stream().map(this::toPayment).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long schoolId, Long id) {
        return toPayment(paymentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", id)));
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
