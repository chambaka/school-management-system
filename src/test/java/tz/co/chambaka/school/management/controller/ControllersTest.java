package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AllocationRequest;
import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.SchoolClassRequest;
import tz.co.chambaka.school.management.dto.academic.SectionRequest;
import tz.co.chambaka.school.management.dto.academic.DepartmentRequest;
import tz.co.chambaka.school.management.dto.academic.SubjectRequest;
import tz.co.chambaka.school.management.dto.academic.TimetableRequest;
import tz.co.chambaka.school.management.dto.attendance.MarkStudentAttendanceRequest;
import tz.co.chambaka.school.management.dto.attendance.MarkTeacherAttendanceRequest;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.ResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeResponse;
import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.dto.notice.NoticeRequest;
import tz.co.chambaka.school.management.dto.parent.CreateParentRequest;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.dto.school.CreateSchoolRequest;
import tz.co.chambaka.school.management.dto.school.UpdateSchoolRequest;
import tz.co.chambaka.school.management.dto.tenant.CreateTenantRequest;
import tz.co.chambaka.school.management.dto.tenant.RenameOrganizationRequest;
import tz.co.chambaka.school.management.dto.tenant.UpdateTenantRequest;
import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.teacher.CreateTeacherRequest;
import tz.co.chambaka.school.management.dto.teacher.UpdateTeacherRequest;
import tz.co.chambaka.school.management.model.enums.ExamType;
import tz.co.chambaka.school.management.model.enums.FeeFrequency;
import tz.co.chambaka.school.management.model.enums.FeeType;
import tz.co.chambaka.school.management.model.enums.NoticeAudience;
import tz.co.chambaka.school.management.model.enums.PaymentMethod;
import tz.co.chambaka.school.management.model.enums.RelationshipType;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.audit.AuditQueryService;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.service.AcademicYearService;
import tz.co.chambaka.school.management.service.AllocationService;
import tz.co.chambaka.school.management.service.AttendanceService;
import tz.co.chambaka.school.management.service.AuthService;
import tz.co.chambaka.school.management.service.ClassService;
import tz.co.chambaka.school.management.service.ExamService;
import tz.co.chambaka.school.management.service.FinanceService;
import tz.co.chambaka.school.management.service.GradeService;
import tz.co.chambaka.school.management.service.NoticeService;
import tz.co.chambaka.school.management.service.StudentCommunicationService;
import tz.co.chambaka.school.management.service.ParentService;
import tz.co.chambaka.school.management.service.PasswordResetService;
import tz.co.chambaka.school.management.service.SchoolAdminService;
import tz.co.chambaka.school.management.service.SchoolService;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.service.TenantService;
import tz.co.chambaka.school.management.service.SectionService;
import tz.co.chambaka.school.management.service.StudentService;
import tz.co.chambaka.school.management.service.DepartmentService;
import tz.co.chambaka.school.management.service.SubjectService;
import tz.co.chambaka.school.management.service.TeacherService;
import tz.co.chambaka.school.management.service.TimetableService;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.support.Fixtures;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllersTest {

    @Mock
    private TenantResolver tenantResolver;
    @Mock
    private AuthService authService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private SchoolService schoolService;
    @Mock
    private TenantService tenantService;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private ClassService classService;
    @Mock
    private SectionService sectionService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private SchoolAdminService schoolAdminService;
    @Mock
    private StudentService studentService;
    @Mock
    private ParentService parentService;
    @Mock
    private AllocationService allocationService;
    @Mock
    private TimetableService timetableService;
    @Mock
    private ExamService examService;
    @Mock
    private GradeService gradeService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private FinanceService financeService;
    @Mock
    private NoticeService noticeService;
    @Mock
    private StudentCommunicationService communicationService;
    @Mock
    private AuditQueryService auditQueryService;
    @Mock
    private tz.co.chambaka.school.management.audit.AuditActionSettingsService auditActionSettingsService;

    @BeforeEach
    void tenant() {
        org.mockito.Mockito.lenient().when(tenantResolver.requireSchoolId()).thenReturn(1L);
        org.mockito.Mockito.lenient().when(tenantResolver.requireTenantId()).thenReturn(10L);
        org.mockito.Mockito.lenient().when(tenantResolver.resolve(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1L);
    }

    @Test
    void authAndBrandingAndSchool() {
        AuthController auth = new AuthController(authService, passwordResetService);
        auth.login(new LoginRequest("a@b.com", "pw"));
        auth.registerSchool(new RegisterSchoolRequest(null, "a@b.com", "HaloCampus1!", "A", null, null, null, null, "Org", null));
        auth.refresh(new RefreshTokenRequest("rt"));
        UserPrincipal admin = Fixtures.principal(Role.ADMIN);
        auth.switchSchool(admin, new tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest(1L, 20L));
        auth.me(admin);
        auth.changePassword(admin, new ChangePasswordRequest("old", "HaloCampus1!"));
        when(passwordResetService.requestReset(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ForgotPasswordResponse("ok", "a***@b.com", 1800, "123456"));
        when(passwordResetService.verifyCode(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new VerifyResetCodeResponse("sess", 1800));
        assertThat(auth.passwordRules().minLength()).isEqualTo(10);
        auth.forgotPassword(new ForgotPasswordRequest("a@b.com"));
        auth.verifyResetCode(new VerifyResetCodeRequest("a@b.com", "123456"));
        auth.resetPassword(new ResetPasswordRequest("sess", "HaloCampus1!"));
        verify(authService).me(10L);
        verify(passwordResetService).resetPassword(org.mockito.ArgumentMatchers.any());

        BrandingController branding = new BrandingController(schoolService);
        branding.bySlug("chambaka");
        when(schoolService.brandingByHost("school.test")).thenReturn(Fixtures.branding());
        assertThat(branding.byHost("school.test").getStatusCode()).isEqualTo(HttpStatus.OK);
        when(schoolService.brandingByHost("localhost:5174"))
                .thenThrow(new ResourceNotFoundException("No school is mapped to this domain"));
        assertThat(branding.byHost("localhost:5174").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        when(schoolService.brandingByHost("missing.school"))
                .thenThrow(new ResourceNotFoundException("No school is mapped to this domain"));
        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class, () -> branding.byHost("missing.school"));
        PublicConfigController publicConfig = new PublicConfigController(Fixtures.properties(), tenantRepository);
        assertThat(publicConfig.config().tenancyMode()).isEqualTo("multi");
        assertThat(publicConfig.config().registrationEnabled()).isTrue();
        assertThat(publicConfig.config().organizationName()).isNull();
        SmsProperties singleProps = SmsProperties.of(
                Fixtures.properties().jwt(),
                Fixtures.properties().cors(),
                Fixtures.properties().superAdmin(),
                Fixtures.properties().passwordReset(),
                new SmsProperties.Tenancy(SmsProperties.Mode.SINGLE, "Halo Campus", "halo"));
        when(tenantRepository.findBySlug("halo")).thenReturn(java.util.Optional.of(Fixtures.tenant()));
        PublicConfigController singleConfig = new PublicConfigController(singleProps, tenantRepository);
        assertThat(singleConfig.config().tenancyMode()).isEqualTo("single");
        assertThat(singleConfig.config().registrationEnabled()).isFalse();
        assertThat(singleConfig.config().organizationName()).isEqualTo(Fixtures.tenant().getName());
        when(tenantRepository.findBySlug("halo")).thenReturn(java.util.Optional.empty());
        assertThat(new PublicConfigController(singleProps, tenantRepository).config().organizationName())
                .isEqualTo("Halo Campus");

        SchoolController schools = new SchoolController(schoolService, tenantResolver);
        schools.listAll();
        schools.current(admin);
        schools.updateCurrent(new UpdateSchoolRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null));
        schools.updatePlatform(1L, new UpdateSchoolRequest("N", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null));
        schools.archive(1L);
        verify(schoolService).list();
        verify(schoolService).archive(1L);

        AuditEventController tenantAudit = new AuditEventController(auditQueryService, tenantResolver);
        tenantAudit.list(null, null, null, null, null, null, PageRequest.of(0, 10));
        tenantAudit.byCorrectionId("corr-1");
        PlatformAuditController platformAudit = new PlatformAuditController(auditQueryService);
        platformAudit.list(AuditScope.PLATFORM, 1L, "corr-1", AuditAction.LOGIN, "Auth", "a@b.com",
                null, null, PageRequest.of(0, 10));
        platformAudit.byCorrectionId("corr-1");
        verify(auditQueryService).byCorrectionId("corr-1");
        verify(auditQueryService).byCorrectionIdForSchool("corr-1", 1L);

        PlatformAuditSettingsController settings = new PlatformAuditSettingsController(auditActionSettingsService);
        when(auditActionSettingsService.list()).thenReturn(
                new tz.co.chambaka.school.management.dto.audit.AuditActionSettingsResponse(List.of()));
        when(auditActionSettingsService.update(eq(AuditAction.PAYMENT_RECORDED), eq(false))).thenReturn(
                new tz.co.chambaka.school.management.dto.audit.AuditActionSettingResponse(
                        AuditAction.PAYMENT_RECORDED, "Finance", "Payment posted", "posted", false));
        assertThat(settings.list().events()).isEmpty();
        assertThat(settings.update(AuditAction.PAYMENT_RECORDED,
                new tz.co.chambaka.school.management.dto.audit.UpdateAuditActionSettingRequest(false)).enabled())
                .isFalse();
        settings.replaceAll(new tz.co.chambaka.school.management.dto.audit.ReplaceAuditActionSettingsRequest(List.of(
                new tz.co.chambaka.school.management.dto.audit.ReplaceAuditActionSettingsRequest.Item(
                        AuditAction.LOGIN, true))));

        verify(auditActionSettingsService).replaceAll(any());
    }

    @Test
    void structurePeopleAcademics() {
        AcademicYearController years = new AcademicYearController(academicYearService, tenantResolver);
        AcademicYearRequest yearReq = new AcademicYearRequest("2026", LocalDate.now(), LocalDate.now().plusDays(1), true);
        years.list();
        years.create(yearReq);
        years.update(1L, yearReq);
        years.setCurrent(1L);

        ClassController classes = new ClassController(classService, tenantResolver);
        SchoolClassRequest classReq = new SchoolClassRequest(1L, "F1", "F1", 1);
        classes.list(null);
        classes.create(classReq);
        classes.update(1L, classReq);
        classes.delete(1L);

        SectionController sections = new SectionController(sectionService, tenantResolver);
        SectionRequest sectionReq = new SectionRequest(1L, "A", 40, 1L);
        sections.list(1L);
        sections.create(sectionReq);
        sections.update(1L, sectionReq);
        sections.delete(1L);

        SubjectController subjects = new SubjectController(subjectService, tenantResolver);
        SubjectRequest subjectReq = new SubjectRequest("Math", "M", null);
        subjects.list();
        subjects.create(subjectReq);
        subjects.update(1L, subjectReq);
        subjects.delete(1L);

        DepartmentController departments = new DepartmentController(departmentService, tenantResolver);
        DepartmentRequest departmentReq = new DepartmentRequest("Science", null);
        departments.list();
        departments.create(departmentReq);
        departments.update(1L, departmentReq);

        TeacherController teachers = new TeacherController(teacherService, tenantResolver);
        when(teacherService.requireByUser(10L)).thenReturn(Fixtures.teacher());
        teachers.list(PageRequest.of(0, 10));
        teachers.me(Fixtures.principal(Role.TEACHER));
        teachers.get(1L);
        teachers.create(new CreateTeacherRequest("T", "t@x.com", "password1", null, "E1", null, null, null, null));
        teachers.update(1L, new UpdateTeacherRequest(null, null, null, null, null, null, null));

        SchoolAdminController schoolAdmins = new SchoolAdminController(schoolAdminService, tenantResolver);
        schoolAdmins.list(PageRequest.of(0, 10));
        schoolAdmins.get(5L);
        schoolAdmins.create(new CreateSchoolAdminRequest("Asha", "asha@x.com", "HaloCampus1!", "07"));
        schoolAdmins.update(5L, new UpdateSchoolAdminRequest("Asha", "08", true));
        verify(schoolAdminService).create(eq(1L), any());

        StudentController students = new StudentController(studentService, parentService, gradeService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        students.list(null, PageRequest.of(0, 10));
        students.me(Fixtures.principal(Role.STUDENT));
        students.get(1L);
        students.create(new CreateStudentRequest("S", "s@x.com", "password1", null,
                null, null, null, null, null, null, null, null, null, null));
        students.update(1L, new UpdateStudentRequest(null, null, null, null, null, null, null, null, null, null, null, null));
        students.linkParent(1L, new LinkParentRequest(1L, RelationshipType.MOTHER, true));
        students.parents(1L);
        students.reportCard(Fixtures.principal(Role.ADMIN), 1L, 1L);
        students.reportCard(Fixtures.principal(Role.PARENT), 1L, 1L);
        verify(parentService).assertLinked(10L, 1L);
        students.myReportCard(Fixtures.principal(Role.STUDENT), 1L);
        verify(gradeService, times(3)).reportCard(1L, 1L, 1L);

        ParentController parents = new ParentController(parentService, tenantResolver);
        when(parentService.requireByUser(10L)).thenReturn(Fixtures.parent());
        parents.list(PageRequest.of(0, 10));
        parents.create(new CreateParentRequest("P", "p@x.com", "password1", null, null, null));
        parents.myChildren(Fixtures.principal(Role.PARENT));
    }

    @Test
    void examsAttendanceFinanceNotices() {
        AllocationController allocations = new AllocationController(allocationService, tenantResolver);
        allocations.list(1L, null);
        allocations.create(new AllocationRequest(1L, 1L, 1L, 1L, 1L));
        allocations.delete(1L);

        TimetableController timetable = new TimetableController(timetableService, tenantResolver);
        timetable.bySection(1L);
        timetable.byTeacher(1L);
        timetable.create(new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "R"));
        timetable.delete(1L);

        ExamController exams = new ExamController(examService, tenantResolver);
        exams.list(null);
        exams.create(new ExamRequest(1L, 1L, "Mid", ExamType.MIDTERM, LocalDate.now(), LocalDate.now().plusDays(1)));
        exams.publish(1L, true);
        exams.subjects(1L);
        exams.addSubject(1L, new ExamSubjectRequest(1L, BigDecimal.TEN, BigDecimal.ONE, null));

        GradeController grades = new GradeController(gradeService, tenantResolver);
        grades.byExam(1L);
        grades.record(Fixtures.principal(Role.TEACHER), new GradeRequest(1L, 1L, 1L, BigDecimal.TEN, null));

        AttendanceController attendance = new AttendanceController(attendanceService, studentService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        attendance.markStudents(Fixtures.principal(Role.TEACHER), new MarkStudentAttendanceRequest(1L, LocalDate.now(), List.of()));
        attendance.markTeachers(Fixtures.principal(Role.ADMIN), new MarkTeacherAttendanceRequest(LocalDate.now(), List.of()));
        attendance.dailyStudents(1L, LocalDate.now());
        attendance.dailyTeachers(LocalDate.now());
        attendance.studentSummary(1L, LocalDate.now(), LocalDate.now());
        attendance.mySummary(Fixtures.principal(Role.STUDENT), LocalDate.now(), LocalDate.now());

        FeeController fees = new FeeController(financeService, tenantResolver);
        fees.list(1L);
        fees.create(new FeeStructureRequest(1L, 1L, "T", FeeType.TUITION, FeeFrequency.TERM, BigDecimal.TEN, null));

        InvoiceController invoices = new InvoiceController(financeService, studentService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        when(financeService.outstandingBalance(eq(1L), eq(1L), any())).thenReturn(BigDecimal.TEN);
        invoices.list(PageRequest.of(0, 10));
        invoices.generate(new GenerateInvoicesRequest(1L, 1L, List.of(1L), null));
        invoices.get(Fixtures.principal(Role.ADMIN), 1L);
        invoices.byStudent(Fixtures.principal(Role.PARENT), 1L);
        assertThat(invoices.balance(Fixtures.principal(Role.PARENT), 1L)).containsEntry("outstanding", BigDecimal.TEN);
        invoices.mine(Fixtures.principal(Role.STUDENT));

        PaymentController payments = new PaymentController(financeService, tenantResolver);
        payments.record(Fixtures.principal(Role.ADMIN), new RecordPaymentRequest(1L, BigDecimal.ONE, PaymentMethod.CASH, null));
        payments.get(Fixtures.principal(Role.STUDENT), 1L);
        payments.byInvoice(Fixtures.principal(Role.PARENT), 1L);

        NoticeController notices = new NoticeController(noticeService, tenantResolver);
        NoticeRequest noticeReq = new NoticeRequest("T", "C", NoticeAudience.ALL, null, true, null, null);
        notices.list(Fixtures.principal(Role.ADMIN));
        notices.list(Fixtures.principal(Role.SUPER_ADMIN));
        notices.list(Fixtures.principal(Role.TENANT_ADMIN));
        notices.list(Fixtures.principal(Role.STUDENT));
        notices.create(Fixtures.principal(Role.ADMIN), noticeReq);
        notices.update(1L, noticeReq);
        verify(noticeService).listForAudience(eq(1L), eq(Role.STUDENT));

        StudentCommunicationController messages = new StudentCommunicationController(communicationService, tenantResolver);
        messages.list(Fixtures.principal(Role.TEACHER), 1L);
        messages.post(Fixtures.principal(Role.ADMIN), 1L,
                new tz.co.chambaka.school.management.dto.communication.CreateStudentMessageRequest("Please come in.", true));
        messages.inbox(Fixtures.principal(Role.PARENT));
        verify(communicationService).inbox(eq(1L), any());

        TenantController tenants = new TenantController(tenantService, tenantResolver);
        tenants.listAll();
        tenants.create(new CreateTenantRequest("Org", null, null, null, null, null));
        tenants.get(10L);
        tenants.update(10L, new UpdateTenantRequest(null, null, null, null, null, null, null, null));
        tenants.platformSchools(10L);
        tenants.platformAddSchool(10L, new CreateSchoolRequest("S", "Main", null, null, null, null, null));
        tenants.current();
        tenants.updateCurrent(new RenameOrganizationRequest("Halo Group"));
        tenants.currentSchools();
        tenants.addSchool(new CreateSchoolRequest("S2", null, null, null, null, null, null));
        tenants.archive(10L);
        verify(tenantService).archive(10L);
        verify(tenantService).list();
    }
}
