package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.FeeStructure;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.Payment;
import tz.co.chambaka.school.management.model.enums.FeeFrequency;
import tz.co.chambaka.school.management.model.enums.FeeType;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.PaymentMethod;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private FeeStructureRepository feeStructureRepository;
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
        FeeStructureRequest feeReq = new FeeStructureRequest(1L, 1L, "Tuition", FeeType.TUITION,
                FeeFrequency.TERM, new BigDecimal("250000"), LocalDate.of(2026, 10, 1));
        assertThat(service.createFee(1L, feeReq).name()).isEqualTo("Tuition");

        when(feeStructureRepository.findBySchoolIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(fee()));
        assertThat(service.listFees(1L, 1L)).hasSize(1);

        when(feeStructureRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(fee()));
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(Fixtures.student()));
        when(invoiceRepository.countBySchoolId(1L)).thenReturn(0L);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice saved = inv.getArgument(0);
            saved.setId(1L);
            saved.getItems().forEach(item -> item.setId(1L));
            return saved;
        });
        var invoices = service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(1L), LocalDate.of(2026, 10, 1)));
        assertThat(invoices.getFirst().invoiceNumber()).isEqualTo("INV-1-000001");
        assertThat(invoices.getFirst().balance()).isEqualByComparingTo("250000");

        service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(1L), null));

        Invoice invoice = invoice(InvoiceStatus.PENDING, new BigDecimal("250000"), BigDecimal.ZERO);
        when(invoiceRepository.findBySchoolId(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(invoiceRepository.findBySchoolIdAndStudentId(1L, 1L)).thenReturn(List.of(invoice));
        when(invoiceRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(invoice));
        assertThat(service.listInvoices(1L, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(service.studentInvoices(1L, 1L)).hasSize(1);
        assertThat(service.getInvoice(1L, 1L).status()).isEqualTo(InvoiceStatus.PENDING);

        when(invoiceRepository.findBySchoolIdAndStudentIdAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(invoice));
        assertThat(service.outstandingBalance(1L, 1L)).isEqualByComparingTo("250000");

        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, tz.co.chambaka.school.management.model.enums.Role.ADMIN)));
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

        service.recordPayment(1L, new RecordPaymentRequest(1L, new BigDecimal("150000"), PaymentMethod.CASH, null), 2L);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);

        when(paymentRepository.findBySchoolIdAndInvoiceId(1L, 1L)).thenReturn(List.of(payment(invoice)));
        when(paymentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(payment(invoice)));
        assertThat(service.paymentsForInvoice(1L, 1L)).hasSize(1);
        assertThat(service.getPayment(1L, 1L).method()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void feeWithoutClassAndErrors() {
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));
        service.createFee(1L, new FeeStructureRequest(1L, null, "Exam", FeeType.EXAM,
                FeeFrequency.ONE_TIME, BigDecimal.TEN, null));

        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(feeStructureRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(9L), null)))
                .isInstanceOf(ResourceNotFoundException.class);

        when(feeStructureRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(fee()));
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.generateInvoices(1L, new GenerateInvoicesRequest(1L, 1L, List.of(1L), null)))
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
        assertThatThrownBy(() -> service.getInvoice(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(paymentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPayment(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
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
