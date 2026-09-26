package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.tenant.SchoolOverviewResponse;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.model.enums.TeacherStatus;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrganizationOverviewService {

    private final TenantService tenantService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ExamRepository examRepository;
    private final InvoiceRepository invoiceRepository;

    public OrganizationOverviewService(
            TenantService tenantService,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            ExamRepository examRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.tenantService = tenantService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.examRepository = examRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<SchoolOverviewResponse> overview(Long tenantId) {
        return tenantService.listSchools(tenantId).stream()
                .map(school -> new SchoolOverviewResponse(
                        school.id(),
                        studentRepository.search(school.id(), null, false, StudentStatus.ARCHIVED, PageRequest.of(0, 1)).getTotalElements(),
                        teacherRepository.search(school.id(), false, TeacherStatus.ARCHIVED, PageRequest.of(0, 1)).getTotalElements(),
                        examRepository.findBySchoolIdOrderByStartDateDesc(school.id()).stream().filter(exam -> !exam.isPublished()).count(),
                        outstanding(school.id())
                ))
                .toList();
    }

    private BigDecimal outstanding(Long schoolId) {
        return invoiceRepository.findBySchoolId(schoolId, PageRequest.of(0, 1000)).stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED)
                .map(Invoice::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
