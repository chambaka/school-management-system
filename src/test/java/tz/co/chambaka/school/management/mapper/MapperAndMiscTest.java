package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.config.OpenApiConfig;
import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.dto.academic.ClassroomResponse;
import tz.co.chambaka.school.management.dto.academic.DepartmentResponse;
import tz.co.chambaka.school.management.dto.academic.SubjectResponse;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperAndMiscTest {

    @Test
    void generatedMappers() {
        SchoolMapper schoolMapper = new SchoolMapperImpl();
        SchoolResponse school = schoolMapper.toResponse(Fixtures.school());
        BrandingResponse branding = schoolMapper.toBranding(Fixtures.school());
        assertThat(school.slug()).isEqualTo("chambaka-secondary");
        assertThat(branding.name()).isEqualTo("Chambaka Secondary");

        UserMapper userMapper = new UserMapperImpl();
        UserProfileResponse profile = userMapper.toProfile(Fixtures.user(1L, tz.co.chambaka.school.management.model.enums.Role.ADMIN));
        assertThat(profile.role().name()).isEqualTo("ADMIN");

        AcademicMapper academicMapper = new AcademicMapperImpl();
        AcademicYearResponse year = academicMapper.toYear(Fixtures.year());
        SubjectResponse subject = academicMapper.toSubject(Fixtures.subject());
        DepartmentResponse department = academicMapper.toDepartment(Fixtures.department());
        ClassroomResponse classroom = academicMapper.toClassroom(Fixtures.classroom());
        assertThat(year.name()).isEqualTo("2026/2027");
        assertThat(subject.code()).isEqualTo("MATH");
        assertThat(department.name()).isEqualTo("Science");
        assertThat(classroom.name()).isEqualTo("Lab 1");
        assertThat(classroom.code()).isEqualTo("L1");
    }

    @Test
    void pageResponseAndInvoiceBalanceAndOpenApi() {
        PageResponse<String> page = PageResponse.of(new PageImpl<>(List.of("a", "b")));
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).containsExactly("a", "b");

        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("100"));
        invoice.setPaidAmount(new BigDecimal("40"));
        assertThat(invoice.getBalance()).isEqualByComparingTo("60");

        assertThat(new OpenApiConfig().schoolSmsOpenApi().getInfo().getTitle()).contains("School SMS");
    }
}
