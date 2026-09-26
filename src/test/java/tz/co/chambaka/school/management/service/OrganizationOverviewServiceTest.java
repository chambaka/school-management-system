package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.tenant.UpdateOrganizationProfileRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.model.enums.TeacherStatus;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationOverviewServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    @Test
    void overviewCountsLiveSchoolWork() {
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.of(Fixtures.tenant()));
        when(schoolRepository.findByTenantIdOrderByNameAsc(Fixtures.TENANT_ID)).thenReturn(List.of(Fixtures.school()));
        when(studentRepository.search(eq(Fixtures.SCHOOL_ID), eq(null), eq(false), eq(StudentStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Fixtures.student())));
        when(teacherRepository.search(eq(Fixtures.SCHOOL_ID), eq(false), eq(TeacherStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        Exam unpublished = Fixtures.exam();
        unpublished.setPublished(false);
        when(examRepository.findBySchoolIdOrderByStartDateDesc(Fixtures.SCHOOL_ID)).thenReturn(List.of(unpublished));
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setTotalAmount(new BigDecimal("1000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(new BigDecimal("250"));
        when(invoiceRepository.findBySchoolId(eq(Fixtures.SCHOOL_ID), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(invoice)));

        var rows = service().overview(Fixtures.TENANT_ID);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).students()).isEqualTo(1);
        assertThat(rows.get(0).teachers()).isZero();
        assertThat(rows.get(0).unpublishedExams()).isEqualTo(1);
        assertThat(rows.get(0).outstandingFees()).isEqualByComparingTo("750");
    }

    @Test
    void profileAndSchoolStatus() {
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.of(Fixtures.tenant()));
        when(schoolRepository.findByTenantIdOrderByNameAsc(Fixtures.TENANT_ID)).thenReturn(List.of());
        var tenant = tenants();
        var updated = tenant.updateProfile(Fixtures.TENANT_ID, new UpdateOrganizationProfileRequest(
                "org@x.com", "0753493500", "Tanzania", "Africa/Dar_es_Salaam", "TZS", 2));
        assertThat(updated.email()).isEqualTo("org@x.com");
        assertThat(updated.termsPerYear()).isEqualTo(2);

        when(schoolRepository.findById(Fixtures.SCHOOL_ID)).thenReturn(Optional.of(Fixtures.school()));
        var school = Fixtures.school();
        when(schoolRepository.findById(Fixtures.SCHOOL_ID)).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> tenant.setSchoolStatus(Fixtures.TENANT_ID, Fixtures.SCHOOL_ID, SchoolStatus.ARCHIVED))
                .isInstanceOf(BusinessException.class);
        var suspended = tenant.setSchoolStatus(Fixtures.TENANT_ID, Fixtures.SCHOOL_ID, SchoolStatus.SUSPENDED);
        assertThat(school.getStatus()).isEqualTo(SchoolStatus.SUSPENDED);
        assertThat(suspended.status()).isEqualTo(SchoolStatus.SUSPENDED);
    }

    private OrganizationOverviewService service() {
        return new OrganizationOverviewService(
                tenants(), studentRepository, teacherRepository, examRepository, invoiceRepository);
    }

    private TenantService tenants() {
        return new TenantService(
                tenantRepository,
                schoolRepository,
                null,
                Mappers.getMapper(SchoolMapper.class),
                null,
                null,
                null);
    }
}
