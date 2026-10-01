package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.FeeStructureResponse;
import tz.co.chambaka.school.management.dto.finance.UpdateFeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.InvoiceDiscountRequest;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicTerm;
import tz.co.chambaka.school.management.model.FeeStructure;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.Payment;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.FeeFrequency;
import tz.co.chambaka.school.management.model.enums.FeeType;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.PaymentMethod;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AcademicTermRepository;
import tz.co.chambaka.school.management.repository.FeeStructureRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.PaymentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private FeeStructureRepository feeStructureRepository;
    @Mock
    private AcademicTermRepository academicTermRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private ClassService classService;
    @Mock
    private StudentService studentService;
    @Mock
    private ParentService parentService;
    @Mock
    private AuditService auditService;
    @Mock
    private tz.co.chambaka.school.management.ledger.LedgerService ledgerService;
    @InjectMocks
    private FinanceService service;

    @Test
    void feesInvoicesPayments() {
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> {
            FeeStructure saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(academicTermRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(1L, 1L))
                .thenReturn(List.of(term("Term 1", LocalDate.of(2026, 6, 30))));
        FeeStructureRequest feeReq = new FeeStructureRequest(1L, 1L, "Tuition", FeeType.TUITION,
                FeeFrequency.TERM, new BigDecimal("250000"), LocalDate.of(2026, 10, 1));
        assertThat(service.createFee(1L, feeReq).getFirst().name()).isEqualTo("Tuition");
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.FEE_CREATED), eq("FeeStructure"), eq("1"),
                contains("Tuition"), contains("amount=250000"));

        when(feeStructureRepository.findBySchoolIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(fee()));
        assertThat(service.listFees(1L, 1L)).hasSize(1);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.FEE_LISTED), eq("FeeStructure"), eq("1"),
                contains("1 fee"), any());

        when(feeStructureRepository.findBySchoolId(1L)).thenReturn(List.of(fee()));
        assertThat(service.listFees(1L, null)).hasSize(1);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.FEE_LISTED), eq("FeeStructure"), isNull(),
                contains("all years"), any());

        when(feeStructureRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(fee()));
        when(feeStructureRepository.findBySchoolIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(fee()));
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(Fixtures.student()));
        when(invoiceRepository.countBySchoolId(1L)).thenReturn(0L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(1L);
            saved.getItems().forEach(item -> item.setId(1L));
            return saved;
        });
        var invoices = service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(1L), 4, LocalDate.of(2026, 10, 1)));
        assertThat(invoices.getFirst().invoiceNumber()).isEqualTo("INV-1-000001");
        assertThat(invoices.getFirst().balance()).isEqualByComparingTo("250000");
        assertThat(invoices.getFirst().academicYearId()).isEqualTo(1L);
        assertThat(invoices.getFirst().academicYearName()).isEqualTo("2026/2027");
        assertThat(invoices.getFirst().billingQuarter()).isEqualTo(4);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.INVOICE_GENERATED), eq("Invoice"), eq("1"),
                contains("INV-1-000001"), contains("studentId=1"));

        service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(1L), 4, null));

        Invoice invoice = invoice(InvoiceStatus.PENDING, new BigDecimal("250000"), BigDecimal.ZERO);
        when(invoiceRepository.findBySchoolId(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(invoiceRepository.findBySchoolIdAndStudentId(1L, 1L)).thenReturn(List.of(invoice));
        when(invoiceRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(invoice));
        assertThat(service.listInvoices(1L, PageRequest.of(0, 10)).content()).hasSize(1);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.INVOICE_LISTED), eq("Invoice"), isNull(),
                contains("count=1"), contains("INV-1-000001"));
        assertThat(service.studentInvoices(1L, 1L, Fixtures.principal(Role.HEADMASTER))).hasSize(1);
        assertThat(service.getInvoice(1L, 1L, Fixtures.principal(Role.HEADMASTER)).status())
                .isEqualTo(InvoiceStatus.PENDING);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.INVOICE_VIEWED), eq("Invoice"), eq("1"),
                contains("INV-1-000001"), contains("status=PENDING"));

        when(invoiceRepository.findBySchoolIdAndStudentIdAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(invoice));
        assertThat(service.outstandingBalance(1L, 1L, Fixtures.principal(Role.HEADMASTER)))
                .isEqualByComparingTo("250000");
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.BALANCE_VIEWED), eq("Student"), eq("1"),
                contains("250000"), contains("outstanding=250000"));

        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, Role.HEADMASTER)));
        when(paymentRepository.countBySchoolId(1L)).thenReturn(0L);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        var payment = service.recordPayment(1L, new RecordPaymentRequest(1L, new BigDecimal("100000"),
                PaymentMethod.MOBILE_MONEY, "MPESA"), 2L);
        assertThat(payment.receiptNumber()).isEqualTo("RCPT-1-000001");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.PAYMENT_RECORDED), eq("Payment"), eq("1"),
                contains("PENDING->PARTIAL"), contains("remaining=150000"));

        service.recordPayment(1L, new RecordPaymentRequest(1L, new BigDecimal("150000"), PaymentMethod.CASH, null), 2L);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);

        when(paymentRepository.findBySchoolIdAndInvoiceId(1L, 1L)).thenReturn(List.of(payment(invoice)));
        when(paymentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(payment(invoice)));
        assertThat(service.paymentsForInvoice(1L, 1L, Fixtures.principal(Role.HEADMASTER))).hasSize(1);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.PAYMENT_LISTED), eq("Invoice"), eq("1"),
                contains("payments"), contains("RCPT-1-000001"));
        assertThat(service.getPayment(1L, 1L, Fixtures.principal(Role.HEADMASTER)).method()).isEqualTo(PaymentMethod.CASH);
        verify(auditService).recordFinance(eq(1L), eq(AuditAction.PAYMENT_VIEWED), eq("Payment"), eq("1"),
                contains("RCPT-1-000001"), contains("amount=10"));
    }

    @Test
    void feeWithoutClassAndErrors() {
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));
        service.createFee(1L, new FeeStructureRequest(1L, null, "Exam", FeeType.EXAM,
                FeeFrequency.ONE_TIME, BigDecimal.TEN, LocalDate.of(2026, 10, 1)));

        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(feeStructureRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(9L), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        when(feeStructureRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(fee()));
        when(feeStructureRepository.findBySchoolIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(fee()));
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(1L), null, null)))
                .isInstanceOf(BusinessException.class);

        Invoice cancelled = invoice(InvoiceStatus.CANCELLED, new BigDecimal("10"), BigDecimal.ZERO);
        when(invoiceRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(cancelled));
        assertThatThrownBy(() -> service.recordPayment(1L, new RecordPaymentRequest(2L, BigDecimal.ONE, PaymentMethod.CASH, null), 1L))
                .isInstanceOf(BusinessException.class);

        Invoice pending = invoice(InvoiceStatus.PENDING, new BigDecimal("10"), BigDecimal.ZERO);
        when(invoiceRepository.findByIdAndSchoolId(3L, 1L)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.recordPayment(1L, new RecordPaymentRequest(3L, new BigDecimal("20"), PaymentMethod.CASH, null), 1L))
                .isInstanceOf(BusinessException.class);

        when(invoiceRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getInvoice(1L, 9L, Fixtures.principal(Role.HEADMASTER)))
                .isInstanceOf(ResourceNotFoundException.class);
        when(paymentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPayment(1L, 9L, Fixtures.principal(Role.HEADMASTER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void financeAccessIsBoundToOwnOrLinkedStudent() {
        Invoice invoice = invoice(InvoiceStatus.PENDING, new BigDecimal("10"), BigDecimal.ZERO);
        when(invoiceRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(invoice));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        assertThat(service.getInvoice(1L, 1L, Fixtures.principal(Role.STUDENT)).id()).isEqualTo(1L);

        Student other = Fixtures.student();
        other.setId(99L);
        when(studentService.requireByUser(10L)).thenReturn(other);
        assertThatThrownBy(() -> service.getInvoice(1L, 1L, Fixtures.principal(Role.STUDENT)))
                .isInstanceOf(ResourceNotFoundException.class);

        service.getInvoice(1L, 1L, Fixtures.principal(Role.PARENT));
        verify(parentService).assertLinked(10L, 1L);

        doThrow(new ResourceNotFoundException("Student is not linked to this parent"))
                .when(parentService).assertLinked(10L, 1L);
        assertThatThrownBy(() -> service.getInvoice(1L, 1L, Fixtures.principal(Role.PARENT)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.getInvoice(1L, 1L, Fixtures.principal(Role.TEACHER)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.getInvoice(1L, 1L, Fixtures.principal(Role.SUPER_ADMIN)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.getInvoice(1L, 1L, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void ledgerDiscountAndReceiptReflectAdjustedBalance() {
        Invoice invoice = invoice(InvoiceStatus.PENDING, new BigDecimal("100"), new BigDecimal("20"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        Payment payment = payment(invoice);
        payment.setAmount(new BigDecimal("20"));
        payment.setTransactionRef(null);
        when(invoiceRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(invoice));

        var discounted = service.applyDiscount(1L, 1L,
                new InvoiceDiscountRequest(new BigDecimal("30"), "Scholarship"),
                Fixtures.principal(Role.HEADMASTER));
        assertThat(discounted.discountAmount()).isEqualByComparingTo("30");
        assertThat(discounted.balance()).isEqualByComparingTo("50");

        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(invoiceRepository.findBySchoolIdAndStudentId(1L, 1L)).thenReturn(List.of(invoice));
        when(paymentRepository.findBySchoolIdAndStudentId(1L, 1L)).thenReturn(List.of(payment));
        var ledger = service.ledger(1L, 1L, Fixtures.principal(Role.HEADMASTER));
        assertThat(ledger.discounts()).isEqualByComparingTo("30");
        assertThat(ledger.outstanding()).isEqualByComparingTo("50");

        when(paymentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(payment));
        assertThat(service.receiptPdf(1L, 1L, Fixtures.principal(Role.HEADMASTER)))
                .isNotEmpty().startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
    }

    @Test
    void discountCannotExceedUnpaidAmountAndCanSetInvoicePaid() {
        Invoice invoice = invoice(InvoiceStatus.PENDING, new BigDecimal("100"), new BigDecimal("90"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        when(invoiceRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(invoice));
        assertThatThrownBy(() -> service.applyDiscount(1L, 1L,
                new InvoiceDiscountRequest(new BigDecimal("11"), "Too much"),
                Fixtures.principal(Role.HEADMASTER))).isInstanceOf(BusinessException.class);

        service.applyDiscount(1L, 1L, new InvoiceDiscountRequest(BigDecimal.TEN, "Waiver"),
                Fixtures.principal(Role.HEADMASTER));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void feeLinesFollowFrequencyAndStayEditable() {
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(academicTermRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(1L, 1L)).thenReturn(List.of(
                term("Term 1", LocalDate.of(2026, 6, 30)),
                term("Term 2", LocalDate.of(2026, 12, 15))));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

        List<FeeStructureResponse> termFees = service.createFee(1L, new FeeStructureRequest(
                1L, null, "Tuition", FeeType.TUITION, FeeFrequency.TERM, new BigDecimal("100"), null));
        assertThat(termFees).extracting(FeeStructureResponse::periodLabel).containsExactly("Term 1", "Term 2");
        assertThat(termFees).extracting(FeeStructureResponse::dueDate)
                .containsExactly(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 15));

        when(academicTermRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(1L, 1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.createFee(1L, new FeeStructureRequest(
                1L, null, "Tuition", FeeType.TUITION, FeeFrequency.TERM, new BigDecimal("100"), null)))
                .isInstanceOf(BusinessException.class);

        List<FeeStructureResponse> months = service.createFee(1L, new FeeStructureRequest(
                1L, null, "Transport", FeeType.TRANSPORT, FeeFrequency.MONTHLY, new BigDecimal("20"), null));
        assertThat(months).hasSize(12);
        assertThat(months.getFirst().periodLabel()).isEqualTo("January 2026");
        assertThat(months.getFirst().dueDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(months.get(11).periodLabel()).isEqualTo("December 2026");

        List<FeeStructureResponse> yearly = service.createFee(1L, new FeeStructureRequest(
                1L, null, "Boarding", FeeType.BOARDING, FeeFrequency.YEARLY, new BigDecimal("500"), LocalDate.of(2026, 3, 1)));
        assertThat(yearly).hasSize(1);
        assertThat(yearly.getFirst().dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(yearly.getFirst().periodLabel()).isEqualTo("Year");

        assertThatThrownBy(() -> service.createFee(1L, new FeeStructureRequest(
                1L, null, "Exam", FeeType.EXAM, FeeFrequency.ONE_TIME, BigDecimal.TEN, null)))
                .isInstanceOf(BusinessException.class);

        FeeStructure yearlyFee = fee();
        yearlyFee.setFrequency(FeeFrequency.YEARLY);
        yearlyFee.setPeriodLabel("Year");
        yearlyFee.setDueDate(LocalDate.of(2026, 12, 31));
        when(feeStructureRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(yearlyFee));
        FeeStructureResponse updatedYear = service.updateFee(1L, 1L,
                new UpdateFeeStructureRequest(new BigDecimal("600"), LocalDate.of(2026, 2, 1)));
        assertThat(updatedYear.amount()).isEqualByComparingTo("600");
        assertThat(updatedYear.dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));

        FeeStructure termFee = fee();
        termFee.setId(2L);
        termFee.setPeriodLabel("Term 1");
        termFee.setDueDate(LocalDate.of(2026, 6, 30));
        when(feeStructureRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(termFee));
        FeeStructureResponse updatedTerm = service.updateFee(1L, 2L,
                new UpdateFeeStructureRequest(new BigDecimal("80"), LocalDate.of(2026, 7, 1)));
        assertThat(updatedTerm.amount()).isEqualByComparingTo("80");
        assertThat(updatedTerm.dueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    private static AcademicTerm term(String name, LocalDate endDate) {
        AcademicTerm term = new AcademicTerm();
        term.setName(name);
        term.setEndDate(endDate);
        return term;
    }

    private FeeStructure fee() {
        FeeStructure fee = new FeeStructure();
        fee.setId(1L);
        fee.setAcademicYear(Fixtures.year());
        fee.setSchoolClass(Fixtures.schoolClass());
        fee.setName("Tuition");
        fee.setFeeType(FeeType.TUITION);
        fee.setFrequency(FeeFrequency.TERM);
        fee.setAmount(new BigDecimal("250000"));
        return fee;
    }

    private Invoice invoice(InvoiceStatus status, BigDecimal total, BigDecimal paid) {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setStudent(Fixtures.student());
        invoice.setAcademicYear(Fixtures.year());
        invoice.setInvoiceNumber("INV-1-000001");
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(paid);
        invoice.setStatus(status);
        invoice.setIssuedAt(Instant.now());
        return invoice;
    }

    private Payment payment(Invoice invoice) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setInvoice(invoice);
        payment.setStudent(Fixtures.student());
        payment.setAmount(BigDecimal.TEN);
        payment.setMethod(PaymentMethod.CASH);
        payment.setReceiptNumber("RCPT-1-000001");
        payment.setPaidAt(Instant.now());
        return payment;
    }
}
