package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.dto.student.EnrolmentHistoryResponse;
import tz.co.chambaka.school.management.model.enums.PromotionAction;
import tz.co.chambaka.school.management.model.enums.Role;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock GradeService gradeService;
    @Mock AttendanceService attendanceService;
    @Mock FinanceService financeService;
    @Mock StudentService studentService;
    @Mock PromotionService promotionService;
    @Mock tz.co.chambaka.school.management.repository.InvoiceRepository invoiceRepository;
    @Mock tz.co.chambaka.school.management.repository.PaymentRepository paymentRepository;
    @InjectMocks ReportExportService service;

    @Test
    void exportsReportCardAsPdfAndCsv() {
        when(gradeService.reportCard(1L, 1L, 1L, Role.PARENT)).thenReturn(card(2));

        byte[] pdf = service.reportCardPdf(1L, 1L, 1L, Role.PARENT);
        byte[] csv = service.reportCardCsv(1L, 1L, 1L, Role.PARENT);

        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).startsWith("%PDF-1.4");
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("Subject,Marks,Max,Passed")
                .contains("Mathematics,78,100,Yes");
    }

    @Test
    void pdfHandlesMissingSectionAndPosition() {
        when(gradeService.reportCard(1L, 1L, 1L, Role.STUDENT)).thenReturn(card(null));
        assertThat(service.reportCardPdf(1L, 1L, 1L, Role.STUDENT)).isNotEmpty();
    }

    @Test
    void exportsEnrolmentHistoryAsPdfAndCsv() {
        when(promotionService.history(1L, null, null, null)).thenReturn(List.of(
                new EnrolmentHistoryResponse(
                        9L, 1L, "Student", "ADM-001", PromotionAction.REPEAT,
                        java.time.LocalDate.of(2026, 1, 15), 1L, "2026/2027",
                        1L, "Form 1", 1L, "A", "Repeat year")));
        byte[] pdf = service.enrolmentHistoryPdf(1L, null, null, null);
        byte[] csv = service.enrolmentHistoryCsv(1L, null, null, null);
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).startsWith("%PDF-1.4")
                .contains("Enrolment history")
                .contains("REPEAT");
        assertThat(new String(csv, StandardCharsets.UTF_8))
                .contains("Date,Action,Admission No,Student,Year,Class,Section,Notes")
                .contains("ADM-001")
                .contains("Repeat year");
        when(promotionService.history(1L, 1L, 1L, PromotionAction.PROMOTE)).thenReturn(List.of());
        assertThat(new String(service.enrolmentHistoryPdf(1L, 1L, 1L, PromotionAction.PROMOTE), StandardCharsets.ISO_8859_1))
                .contains("No enrolment history");
    }

    @Test
    void exportsNewReportPack() {
        when(gradeService.reportCard(1L, 1L, 1L, Role.HEADMASTER)).thenReturn(card(1));
        assertThat(service.reportCardXlsx(1L, 1L, 1L, Role.HEADMASTER)).isNotEmpty();

        tz.co.chambaka.school.management.model.Invoice invoice = new tz.co.chambaka.school.management.model.Invoice();
        invoice.setInvoiceNumber("INV-1");
        invoice.setStudent(tz.co.chambaka.school.management.support.Fixtures.student());
        invoice.setTotalAmount(java.math.BigDecimal.TEN);
        invoice.setPaidAmount(java.math.BigDecimal.ONE);
        invoice.setDiscountAmount(java.math.BigDecimal.ZERO);
        invoice.setStatus(tz.co.chambaka.school.management.model.enums.InvoiceStatus.PARTIAL);
        when(invoiceRepository.findBySchoolIdAndStatusIn(any(), any())).thenReturn(List.of(invoice));
        assertThat(new String(service.defaulters(1L, "csv"), StandardCharsets.UTF_8)).contains("INV-1");
        assertThat(service.defaulters(1L, "pdf")).isNotEmpty();
        assertThat(service.defaulters(1L, "xlsx")).isNotEmpty();

        tz.co.chambaka.school.management.model.Payment payment = new tz.co.chambaka.school.management.model.Payment();
        payment.setReceiptNumber("R-1");
        payment.setStudent(tz.co.chambaka.school.management.support.Fixtures.student());
        payment.setAmount(java.math.BigDecimal.TEN);
        payment.setMethod(tz.co.chambaka.school.management.model.enums.PaymentMethod.CASH);
        payment.setPaidAt(java.time.Instant.parse("2026-09-01T00:00:00Z"));
        when(paymentRepository.findBySchoolIdAndPaidAtBetween(any(), any(), any())).thenReturn(List.of(payment));
        assertThat(new String(service.collections(1L, null, null, "csv"), StandardCharsets.UTF_8)).contains("R-1");

        when(gradeService.meritList(1L, 1L, Role.HEADMASTER)).thenReturn(List.of(
                new tz.co.chambaka.school.management.dto.academic.MeritRowResponse(
                        1, 1L, "Student", "ADM-001", java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, "A")));
        assertThat(new String(service.meritList(1L, 1L, Role.HEADMASTER, "csv"), StandardCharsets.UTF_8)).contains("ADM-001");

        when(attendanceService.schoolSummaries(any(), any(), any())).thenReturn(List.of(
                new tz.co.chambaka.school.management.dto.attendance.AttendanceSummaryResponse(
                        1L, "Student", 8, 1, 1, 0, 10, 90.0)));
        assertThat(new String(service.attendanceSummary(1L, java.time.LocalDate.now(), java.time.LocalDate.now(), "csv"),
                StandardCharsets.UTF_8)).contains("Student");
        assertThat(service.attendanceSummary(1L, java.time.LocalDate.now(), java.time.LocalDate.now(), "pdf")).isNotEmpty();
    }

    private ReportCardResponse card(Integer position) {
        GradeResponse subject = new GradeResponse(
                1L, 1L, 1L, "Student", 1L, "Mathematics",
                BigDecimal.valueOf(78), BigDecimal.valueOf(100), BigDecimal.valueOf(40), true, "Good");
        return new ReportCardResponse(
                1L, "Student", "ADM-001", "Form 1", position == null ? null : "A",
                1L, "Midterm", List.of(subject), BigDecimal.valueOf(78), BigDecimal.valueOf(100),
                BigDecimal.valueOf(78), "B", BigDecimal.valueOf(4), position, true);
    }
}
