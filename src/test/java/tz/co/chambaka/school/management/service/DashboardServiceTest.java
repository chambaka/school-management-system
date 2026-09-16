package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.enums.ExamApprovalStatus;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.NoticeRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock NoticeRepository noticeRepository;
    @Mock ExamRepository examRepository;
    @InjectMocks DashboardService service;

    @BeforeEach
    void data() {
        when(studentRepository.search(any(), any(), any(Boolean.class), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Fixtures.student(), Fixtures.student())));
        when(teacherRepository.search(any(), any(Boolean.class), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Fixtures.teacher())));
        when(invoiceRepository.countBySchoolId(1L)).thenReturn(2L);
        when(noticeRepository.findBySchoolIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(examRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(
                exam(ExamApprovalStatus.ENTERED, false),
                exam(ExamApprovalStatus.VERIFIED, false),
                exam(ExamApprovalStatus.APPROVED, true)));
        when(invoiceRepository.findBySchoolId(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                invoice(InvoiceStatus.PENDING, "100"),
                invoice(InvoiceStatus.PARTIAL, "50"),
                invoice(InvoiceStatus.CANCELLED, "999"))));
    }

    @Test
    void headmasterSeesAcademicAndFinanceTasks() {
        var snapshot = service.snapshot(1L, Role.HEADMASTER);
        assertThat(snapshot.students()).isEqualTo(2);
        assertThat(snapshot.pendingApprovals()).isEqualTo(2);
        assertThat(snapshot.outstandingFees()).isEqualByComparingTo("150");
        assertThat(snapshot.tasks()).hasSize(3);
    }

    @Test
    void roleSpecificTasksAreGenerated() {
        assertThat(service.snapshot(1L, Role.ACADEMIC_MASTER).tasks()).hasSize(2);
        assertThat(service.snapshot(1L, Role.ACCOUNTANT).tasks()).containsExactly("Outstanding fees: 150 TZS");
        assertThat(service.snapshot(1L, Role.TEACHER).tasks()).containsExactly(
                "Mark today's register and enter pending grades");
        assertThat(service.snapshot(1L, Role.STUDENT).tasks()).isEmpty();
    }

    private Exam exam(ExamApprovalStatus status, boolean published) {
        Exam exam = Fixtures.exam();
        exam.setApprovalStatus(status);
        exam.setPublished(published);
        return exam;
    }

    private Invoice invoice(InvoiceStatus status, String balance) {
        Invoice invoice = new Invoice();
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(balance));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        return invoice;
    }
}
