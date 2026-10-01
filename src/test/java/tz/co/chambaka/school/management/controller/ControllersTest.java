package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AllocationRequest;
import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.SchoolClassRequest;
import tz.co.chambaka.school.management.dto.academic.SectionRequest;
import tz.co.chambaka.school.management.dto.academic.ClassroomRequest;
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
import tz.co.chambaka.school.management.dto.auth.VerifyTwoFactorRequest;
import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.UpdateFeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.dto.notice.NoticeRequest;
import tz.co.chambaka.school.management.dto.parent.CreateParentRequest;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.dto.parent.UpdateParentRequest;
import tz.co.chambaka.school.management.dto.school.CreateSchoolRequest;
import tz.co.chambaka.school.management.dto.school.RenameSchoolRequest;
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
import tz.co.chambaka.school.management.service.PromotionService;
import tz.co.chambaka.school.management.service.SchoolAdminService;
import tz.co.chambaka.school.management.service.SchoolService;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.service.TenantService;
import tz.co.chambaka.school.management.service.SectionService;
import tz.co.chambaka.school.management.service.StoredPhoto;
import tz.co.chambaka.school.management.service.StudentService;
import tz.co.chambaka.school.management.service.ClassroomService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private ClassroomService classroomService;
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
    private tz.co.chambaka.school.management.service.GradeImportService gradeImportService;
    @Mock
    private PromotionService promotionService;
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
    @Mock
    private tz.co.chambaka.school.management.service.TwoFactorService twoFactorService;
    @Mock
    private tz.co.chambaka.school.management.service.AcademicTermService academicTermService;
    @Mock
    private tz.co.chambaka.school.management.service.ResultConfigService resultConfigService;
    @Mock
    private tz.co.chambaka.school.management.service.AssignmentService assignmentService;
    @Mock
    private tz.co.chambaka.school.management.service.LessonLogService lessonLogService;
    @Mock
    private tz.co.chambaka.school.management.service.BellPeriodService bellPeriodService;
    @Mock
    private tz.co.chambaka.school.management.service.ReportExportService reportExportService;
    @Mock
    private tz.co.chambaka.school.management.service.AlertService alertService;
    @Mock
    private tz.co.chambaka.school.management.service.DashboardService dashboardService;

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
        auth.verifyTwoFactor(new VerifyTwoFactorRequest("pending", "123456"));
        verify(authService).verifyTwoFactor(org.mockito.ArgumentMatchers.any());
        auth.registerSchool(new RegisterSchoolRequest(null, "a@b.com", "HaloCampus1!", "A", null, null, null, null, "Org", null));
        auth.refresh(new RefreshTokenRequest("rt"));
        UserPrincipal admin = Fixtures.principal(Role.HEADMASTER);
        auth.switchSchool(admin, new tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest(1L, 20L));
        auth.me(admin);
        auth.changePassword(admin, new ChangePasswordRequest("old", "HaloCampus1!"));
        UserController users = new UserController(authService);
        users.unlock(admin, 4L, 1L);
        verify(authService).unlock(4L, 1L, admin);
        users.resetTwoFactor(admin, 4L, 1L);
        verify(authService).resetTwoFactor(4L, 1L, admin);
        users.resetPassword(admin, 4L, 1L, new tz.co.chambaka.school.management.dto.auth.AdminResetPasswordRequest(null));
        verify(authService).resetPassword(eq(4L), eq(1L), eq(admin), any());
        users.list(admin, 1L, null, org.springframework.data.domain.PageRequest.of(0, 20));
        verify(authService).listSchoolUsers(eq(1L), org.mockito.ArgumentMatchers.isNull(), eq(admin), any());
        users.links(admin, 4L, 1L);
        verify(authService).links(4L, 1L, admin);
        users.removeLink(admin, 4L, 1L, new tz.co.chambaka.school.management.dto.admin.RemoveUserLinkRequest("ALLOCATION", 8L, null, null));
        verify(authService).removeLink(eq(4L), eq(1L), eq(admin), any());
        users.setEnabled(admin, 4L, 1L, new tz.co.chambaka.school.management.dto.admin.SetUserEnabledRequest(false));
        verify(authService).setEnabled(eq(4L), eq(1L), eq(admin), any());
        users.remove(admin, 4L, 1L);
        verify(authService).remove(4L, 1L, admin);
        auth.resetOwnTwoFactor(admin);
        verify(authService).resetOwnTwoFactor(10L);
        when(passwordResetService.requestReset(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ForgotPasswordResponse("ok", "a***@b.com", 1800, "123456"));
        when(passwordResetService.verifyCode(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new VerifyResetCodeResponse("sess", 1800));
        assertThat(auth.passwordRules().minLength()).isEqualTo(10);
        auth.forgotPassword(new ForgotPasswordRequest("a@b.com"));
        auth.verifyResetCode(new VerifyResetCodeRequest("a@b.com", "123456"));
        auth.resetPassword(new ResetPasswordRequest("sess", "HaloCampus1!"));
        auth.logout(new RefreshTokenRequest("rt"));
        verify(authService).logout(any());
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
                new SmsProperties.Tenancy(SmsProperties.Mode.SINGLE, "ShuleHub", "shulehub"));
        when(tenantRepository.findBySlug("shulehub")).thenReturn(java.util.Optional.of(Fixtures.tenant()));
        PublicConfigController singleConfig = new PublicConfigController(singleProps, tenantRepository);
        assertThat(singleConfig.config().tenancyMode()).isEqualTo("single");
        assertThat(singleConfig.config().registrationEnabled()).isFalse();
        assertThat(singleConfig.config().organizationName()).isEqualTo(Fixtures.tenant().getName());
        when(tenantRepository.findBySlug("shulehub")).thenReturn(java.util.Optional.empty());
        assertThat(new PublicConfigController(singleProps, tenantRepository).config().organizationName())
                .isEqualTo("ShuleHub");

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

        PlatformTwoFactorController twoFactor = new PlatformTwoFactorController(twoFactorService);
        when(twoFactorService.settings()).thenReturn(
                new tz.co.chambaka.school.management.dto.auth.TwoFactorSettingsResponse(
                        "ShuleHub 2FA", "desc", List.of()));
        when(twoFactorService.updateEnabled(1L, true)).thenReturn(
                new tz.co.chambaka.school.management.dto.auth.SchoolTwoFactorResponse(1L, "Chambaka", "Org", true));
        assertThat(twoFactor.get().schools()).isEmpty();
        assertThat(twoFactor.update(1L, new tz.co.chambaka.school.management.dto.auth.UpdateTwoFactorSettingsRequest(true)).enabled())
                .isTrue();
    }

    @Test
    void structurePeopleAcademics() {
        AcademicYearController years = new AcademicYearController(academicYearService, tenantResolver);
        AcademicYearRequest yearReq = new AcademicYearRequest("2026", LocalDate.now(), LocalDate.now().plusDays(1), true);
        years.list();
        years.create(yearReq);
        years.update(1L, yearReq);
        years.setCurrent(1L);
        years.delete(1L);

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

        ClassroomController classrooms = new ClassroomController(classroomService, tenantResolver);
        ClassroomRequest classroomReq = new ClassroomRequest("Lab 1", "L1", 40, "Block A", null);
        classrooms.list();
        classrooms.create(classroomReq);
        classrooms.update(1L, classroomReq);
        classrooms.delete(1L);

        TeacherController teachers = new TeacherController(teacherService, tenantResolver);
        when(teacherService.requireByUser(10L)).thenReturn(Fixtures.teacher());
        teachers.list(false, PageRequest.of(0, 10));
        teachers.archive(1L);
        teachers.restore(1L);
        teachers.me(Fixtures.principal(Role.TEACHER));
        teachers.get(1L);
        teachers.create(new CreateTeacherRequest("T", "t@x.com", "password1", null, "E1", null, null, null, null));
        teachers.update(1L, new UpdateTeacherRequest(null, null, null, null, null, null, null));
        when(teacherService.photoFile(1L, 1L)).thenReturn(new StoredPhoto(Path.of("t.jpg"), "image/jpeg"));
        teachers.uploadPhoto(1L, new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1}));
        teachers.deletePhoto(1L);
        teachers.myPhoto(Fixtures.principal(Role.TEACHER));
        teachers.photo(Fixtures.principal(Role.HEADMASTER), 1L);
        teachers.photo(Fixtures.principal(Role.TEACHER), 1L);
        var otherTeacher = Fixtures.teacher();
        otherTeacher.setId(99L);
        when(teacherService.requireByUser(10L)).thenReturn(otherTeacher);
        assertThatThrownBy(() -> teachers.photo(Fixtures.principal(Role.TEACHER), 1L))
                .isInstanceOf(AccessDeniedException.class);

        SchoolAdminController schoolAdmins = new SchoolAdminController(schoolAdminService);
        var platform = Fixtures.principal(Role.SUPER_ADMIN);
        schoolAdmins.list(1L, platform, PageRequest.of(0, 10));
        schoolAdmins.get(1L, 5L, platform);
        schoolAdmins.create(1L, platform, new CreateSchoolAdminRequest("Asha", "asha@x.com", "HaloCampus1!", "07", Role.HEADMASTER));
        schoolAdmins.update(1L, 5L, platform, new UpdateSchoolAdminRequest("Asha", "08", true, null));
        verify(schoolAdminService, times(2)).assertCanView(1L, platform);
        verify(schoolAdminService, times(2)).assertCanManage(1L, platform);
        verify(schoolAdminService).create(eq(1L), any());

        StudentController students = new StudentController(studentService, parentService, gradeService, promotionService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        students.list(null, false, PageRequest.of(0, 10));
        students.suspend(1L);
        students.archive(1L);
        students.restore(1L);
        students.unlinkParent(1L, 1L);
        students.me(Fixtures.principal(Role.STUDENT));
        students.get(1L);
        students.create(new CreateStudentRequest("S", "s@x.com", "password1", null,
                null, null, null, null, null, null, null, null, null, null, null));
        students.update(1L, new UpdateStudentRequest(null, null, null, null, null, null, null, null, null, null, null, null, null));
        students.linkParent(1L, new LinkParentRequest(1L, RelationshipType.MOTHER, true));
        students.parents(1L);
        students.reportCard(Fixtures.principal(Role.HEADMASTER), 1L, 1L);
        students.reportCard(Fixtures.principal(Role.PARENT), 1L, 1L);
        verify(parentService).assertLinked(10L, 1L);
        students.myReportCard(Fixtures.principal(Role.STUDENT), 1L);
        verify(gradeService, times(3)).reportCard(eq(1L), eq(1L), eq(1L), any());
        students.termReport(Fixtures.principal(Role.HEADMASTER), 1L, 1L, 1L);
        students.termReport(Fixtures.principal(Role.PARENT), 1L, 1L, null);
        students.myTermReport(Fixtures.principal(Role.STUDENT), 1L, null);
        students.termReports(Fixtures.principal(Role.HEADMASTER), 1L, null);
        verify(gradeService, times(3)).termReport(eq(1L), eq(1L), eq(1L), any(), any());
        verify(gradeService).termReports(1L, 1L, null, Role.HEADMASTER);
        when(studentService.photoFile(1L, 1L)).thenReturn(new StoredPhoto(Path.of("x.jpg"), "image/jpeg"));
        students.uploadPhoto(1L, new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1}));
        students.deletePhoto(1L);
        students.myPhoto(Fixtures.principal(Role.STUDENT));
        students.photo(Fixtures.principal(Role.HEADMASTER), 1L);
        students.photo(Fixtures.principal(Role.TEACHER), 1L);
        students.photo(Fixtures.principal(Role.STUDENT), 1L);
        students.photo(Fixtures.principal(Role.PARENT), 1L);
        verify(parentService, times(3)).assertLinked(10L, 1L);
        var other = Fixtures.student();
        other.setId(99L);
        when(studentService.requireByUser(10L)).thenReturn(other);
        assertThatThrownBy(() -> students.photo(Fixtures.principal(Role.STUDENT), 1L))
                .isInstanceOf(AccessDeniedException.class);
        students.enrolments(null, null, null);
        students.studentEnrolments(1L);
        students.previewPromote(new tz.co.chambaka.school.management.dto.student.PromoteStudentsRequest(
                List.of(1L), tz.co.chambaka.school.management.model.enums.PromotionAction.PROMOTE, 1L, 1L, 1L, null));
        verify(promotionService).history(1L, null, null, null);
        verify(promotionService).history(1L, 1L, null, null);
        verify(promotionService).preview(eq(1L), any());

        ParentController parents = new ParentController(parentService, tenantResolver);
        when(parentService.requireByUser(10L)).thenReturn(Fixtures.parent());
        parents.list(false, PageRequest.of(0, 10));
        parents.archive(1L);
        parents.restore(1L);
        parents.get(1L);
        parents.create(new CreateParentRequest("P", "p@x.com", "password1", null, null, null));
        parents.update(1L, new UpdateParentRequest("P", "07", "Trader", "addr", true));
        parents.children(1L);
        parents.myChildren(Fixtures.principal(Role.PARENT));
    }

    @Test
    void examsAttendanceFinanceNotices() {
        AllocationController allocations = new AllocationController(allocationService, tenantResolver);
        allocations.list(1L, null);
        allocations.mine(Fixtures.principal(Role.TEACHER));
        allocations.create(new AllocationRequest(1L, 1L, 1L, 1L, 1L));
        allocations.update(1L, new AllocationRequest(1L, 1L, 1L, 1L, 1L));
        allocations.delete(1L);

        TimetableController timetable = new TimetableController(timetableService, tenantResolver);
        timetable.bySection(1L);
        timetable.byTeacher(1L);
        timetable.create(new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), "R"));
        timetable.update(1L, new TimetableRequest(1L, 1L, 1L, 1L, DayOfWeek.TUESDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), "R2"));
        timetable.delete(1L);

        ExamController exams = new ExamController(examService, tenantResolver);
        exams.list(Fixtures.principal(Role.HEADMASTER), null);
        exams.create(new ExamRequest(1L, 1L, "Mid", ExamType.MIDTERM, LocalDate.now(), LocalDate.now().plusDays(1)));
        exams.update(1L, new ExamRequest(1L, 1L, "Mid", ExamType.MIDTERM, LocalDate.now(), LocalDate.now().plusDays(1)));
        exams.publish(1L, true);
        exams.subjects(1L);
        exams.addSubject(Fixtures.principal(Role.TEACHER), 1L, new ExamSubjectRequest(1L, BigDecimal.TEN, BigDecimal.ONE, null));
        exams.updateSubject(Fixtures.principal(Role.TEACHER), 1L, 1L, new ExamSubjectRequest(1L, BigDecimal.TEN, BigDecimal.ONE, null));
        exams.deleteSubject(Fixtures.principal(Role.TEACHER), 1L, 1L);
        exams.delete(1L);

        GradeController grades = new GradeController(gradeService, gradeImportService, tenantResolver);
        grades.byExam(1L);
        grades.record(Fixtures.principal(Role.TEACHER), new GradeRequest(1L, 1L, 1L, BigDecimal.TEN, null));
        grades.delete(Fixtures.principal(Role.TEACHER), 1L, 1L, 1L);

        AttendanceController attendance = new AttendanceController(attendanceService, studentService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        attendance.markStudents(Fixtures.principal(Role.TEACHER), new MarkStudentAttendanceRequest(1L, LocalDate.now(), List.of()));
        attendance.markTeachers(Fixtures.principal(Role.HEADMASTER), new MarkTeacherAttendanceRequest(LocalDate.now(), List.of()));
        attendance.dailyStudents(1L, LocalDate.now(), null);
        attendance.dailyTeachers(LocalDate.now());
        attendance.studentSummary(1L, LocalDate.now(), LocalDate.now());
        attendance.mySummary(Fixtures.principal(Role.STUDENT), LocalDate.now(), LocalDate.now());

        FeeController fees = new FeeController(financeService, tenantResolver);
        fees.list(1L);
        fees.list(null);
        fees.create(new FeeStructureRequest(1L, 1L, "T", FeeType.TUITION, FeeFrequency.TERM, BigDecimal.TEN, null));
        fees.update(1L, new UpdateFeeStructureRequest(BigDecimal.TEN, LocalDate.now()));
        fees.delete(1L);

        InvoiceController invoices = new InvoiceController(financeService, studentService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        when(financeService.outstandingBalance(eq(1L), eq(1L), any())).thenReturn(BigDecimal.TEN);
        invoices.list(PageRequest.of(0, 10));
        invoices.generate(new GenerateInvoicesRequest(1L, 1L, List.of(1L), null));
        invoices.removeItem(1L, 2L);
        invoices.get(Fixtures.principal(Role.HEADMASTER), 1L);
        invoices.byStudent(Fixtures.principal(Role.PARENT), 1L);
        assertThat(invoices.balance(Fixtures.principal(Role.PARENT), 1L)).containsEntry("outstanding", BigDecimal.TEN);
        invoices.mine(Fixtures.principal(Role.STUDENT));

        PaymentController payments = new PaymentController(financeService, tenantResolver);
        payments.record(Fixtures.principal(Role.HEADMASTER), new RecordPaymentRequest(1L, BigDecimal.ONE, PaymentMethod.CASH, null));
        payments.get(Fixtures.principal(Role.STUDENT), 1L);
        payments.byInvoice(Fixtures.principal(Role.PARENT), 1L);

        NoticeController notices = new NoticeController(noticeService, tenantResolver);
        NoticeRequest noticeReq = new NoticeRequest("T", "C", NoticeAudience.ALL, null, true, null, null);
        notices.list(Fixtures.principal(Role.HEADMASTER));
        notices.list(Fixtures.principal(Role.SUPER_ADMIN));
        notices.list(Fixtures.principal(Role.HEADMASTER));
        notices.list(Fixtures.principal(Role.STUDENT));
        notices.create(Fixtures.principal(Role.HEADMASTER), noticeReq);
        notices.update(1L, noticeReq);
        verify(noticeService).listForAudience(eq(1L), eq(Role.STUDENT));

        StudentCommunicationController messages = new StudentCommunicationController(communicationService, tenantResolver);
        messages.list(Fixtures.principal(Role.TEACHER), 1L);
        messages.post(Fixtures.principal(Role.HEADMASTER), 1L,
                new tz.co.chambaka.school.management.dto.communication.CreateStudentMessageRequest("Please come in.", true));
        messages.inbox(Fixtures.principal(Role.PARENT));
        verify(communicationService).inbox(eq(1L), any());

        TenantController tenants = new TenantController(
                tenantService,
                org.mockito.Mockito.mock(tz.co.chambaka.school.management.service.OrganizationAdminService.class),
                org.mockito.Mockito.mock(tz.co.chambaka.school.management.service.OrganizationOverviewService.class),
                org.mockito.Mockito.mock(tz.co.chambaka.school.management.audit.AuditQueryService.class),
                tenantResolver);
        tenants.listAll();
        tenants.create(new CreateTenantRequest("Org", null, null, null, null, null));
        tenants.get(10L);
        tenants.update(10L, new UpdateTenantRequest(null, null, null, null, null, null, null, null));
        tenants.platformSchools(10L);
        tenants.platformAddSchool(10L, new CreateSchoolRequest("S", "Main", null, null, null, null, null));
        tenants.current();
        tenants.updateCurrent(new RenameOrganizationRequest("Halo Group"));
        tenants.currentSchools();
        tenants.renameSchool(1L, new RenameSchoolRequest("East Campus"));
        tenants.addSchool(new CreateSchoolRequest("S2", null, null, null, null, null, null));
        tenants.platformAdmins(10L);
        tenants.platformCreateAdmin(10L, new tz.co.chambaka.school.management.dto.tenant.CreateOrganizationAdminRequest("A", "a@b.com", "HaloCampus1!", "07"));
        tenants.currentAdmins();
        tenants.createCurrentAdmin(new tz.co.chambaka.school.management.dto.tenant.CreateOrganizationAdminRequest("A", "a@b.com", null, "07"));
        tenants.archive(10L);
        verify(tenantService).archive(10L);
        verify(tenantService).list();
    }

    @Test
    void mvpControllersAndAdditionalEndpoints() {
        var teacher = Fixtures.principal(Role.TEACHER);
        var headmaster = Fixtures.principal(Role.HEADMASTER);
        var today = LocalDate.of(2026, 9, 16);

        AcademicTermController terms = new AcademicTermController(academicTermService, tenantResolver);
        var termRequest = new tz.co.chambaka.school.management.dto.academic.AcademicTermRequest(
                1L, "Term One", today, today.plusMonths(3), true);
        terms.list(null);
        terms.list(1L);
        terms.create(termRequest);
        terms.update(1L, termRequest);
        terms.delete(1L);

        ResultConfigController resultConfig = new ResultConfigController(resultConfigService, tenantResolver);
        var weights = new tz.co.chambaka.school.management.dto.academic.ResultWeightRequest(
                1L, null, null, BigDecimal.TEN, BigDecimal.valueOf(90),
                BigDecimal.valueOf(50), BigDecimal.valueOf(50));
        resultConfig.weights(1L);
        resultConfig.saveWeight(weights);
        resultConfig.bands();
        resultConfig.replaceBands(List.of(new tz.co.chambaka.school.management.dto.academic.GradingBandRequest(
                80, 100, "A", BigDecimal.valueOf(5), 1)));

        AssignmentController assignments = new AssignmentController(assignmentService, tenantResolver);
        var assignment = new tz.co.chambaka.school.management.dto.academic.AssignmentRequest(
                1L, 1L, 1L, "Algebra", "Complete", today.plusDays(2));
        var file = new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1});
        assignments.list(teacher);
        assignments.create(teacher, assignment);
        assignments.update(teacher, 1L, assignment);
        assignments.lock(teacher, 1L);
        assignments.publish(teacher, 1L);
        assignments.attach(teacher, 1L, file);
        when(assignmentService.file(eq(1L), eq(teacher), eq(1L)))
                .thenReturn(new tz.co.chambaka.school.management.service.StoredPhoto(java.nio.file.Path.of("work.pdf"), "application/pdf"));
        when(assignmentService.submissionFile(eq(1L), eq(teacher), eq(1L), eq(1L)))
                .thenReturn(new tz.co.chambaka.school.management.service.StoredPhoto(java.nio.file.Path.of("essay.pdf"), "application/pdf"));
        assignments.file(teacher, 1L);
        when(assignmentService.file(eq(1L), eq(teacher), eq(1L), eq(2L)))
                .thenReturn(new tz.co.chambaka.school.management.service.StoredPhoto(java.nio.file.Path.of("notes.pdf"), "application/pdf"));
        assignments.file(teacher, 1L, 2L);
        assignments.removeFile(teacher, 1L, 2L);
        assignments.submissionFile(teacher, 1L, 1L);
        when(assignmentService.submissionFile(eq(1L), eq(teacher), eq(1L), eq(1L), eq(3L)))
                .thenReturn(new tz.co.chambaka.school.management.service.StoredPhoto(java.nio.file.Path.of("v2.pdf"), "application/pdf"));
        assignments.submissionAttemptFile(teacher, 1L, 1L, 3L);
        when(assignmentService.submissionFile(eq(1L), eq(teacher), eq(1L), eq(1L), eq(3L), eq(4L)))
                .thenReturn(new tz.co.chambaka.school.management.service.StoredPhoto(java.nio.file.Path.of("scan.pdf"), "application/pdf"));
        assignments.submissionAttemptFile(teacher, 1L, 1L, 3L, 4L);
        assignments.attachMyFile(Fixtures.principal(Role.STUDENT), 1L, file);
        assignments.marks(teacher, 1L, 1L, BigDecimal.TEN);
        assignments.submit(teacher, 1L, "done", file);
        assignments.submissions(teacher, 1L);
        assignments.mySubmissions(Fixtures.principal(Role.STUDENT));
        assignments.mySubmission(Fixtures.principal(Role.STUDENT), 1L);
        assignments.delete(teacher, 1L);

        LessonLogController lessons = new LessonLogController(lessonLogService, tenantResolver);
        var lesson = new tz.co.chambaka.school.management.dto.academic.LessonLogRequest(
                null, 1L, 1L, today, "Fractions", "Add", "Board", "Exercise");
        lessons.list(1L, today);
        lessons.create(teacher, lesson);

        BellPeriodController bells = new BellPeriodController(bellPeriodService, tenantResolver);
        var bell = new tz.co.chambaka.school.management.dto.academic.BellPeriodRequest(
                "P1", LocalTime.of(8, 0), LocalTime.of(9, 0),
                tz.co.chambaka.school.management.model.enums.PeriodKind.LESSON, 1);
        bells.list();
        bells.create(bell);
        bells.update(1L, bell);
        bells.delete(1L);

        when(reportExportService.reportCardPdf(any(), any(), any(), any())).thenReturn(new byte[]{1});
        when(reportExportService.reportCardCsv(any(), any(), any(), any())).thenReturn(new byte[]{2});
        ReportController reports = new ReportController(reportExportService, studentService, tenantResolver);
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        assertThat(reports.reportCardPdf(Fixtures.principal(Role.STUDENT), null, 1L).getBody()).containsExactly(1);
        assertThat(reports.reportCardPdf(headmaster, 1L, 1L).getHeaders().getContentType().toString())
                .isEqualTo("application/pdf");
        assertThat(reports.reportCardCsv(headmaster, 1L, 1L).getBody()).containsExactly(2);
        when(reportExportService.enrolmentHistoryCsv(any(), any(), any(), any())).thenReturn(new byte[]{3});
        when(reportExportService.enrolmentHistoryPdf(any(), any(), any(), any())).thenReturn(new byte[]{4});
        assertThat(reports.enrolmentHistoryCsv(null, null, null).getBody()).containsExactly(3);
        assertThat(reports.enrolmentHistoryPdf(1L, 1L, tz.co.chambaka.school.management.model.enums.PromotionAction.REPEAT)
                .getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        when(reportExportService.reportCardXlsx(any(), any(), any(), any())).thenReturn(new byte[]{5});
        when(reportExportService.defaulters(any(), any())).thenReturn(new byte[]{6});
        when(reportExportService.collections(any(), any(), any(), any())).thenReturn(new byte[]{7});
        when(reportExportService.meritList(any(), any(), any(), any())).thenReturn(new byte[]{8});
        when(reportExportService.attendanceSummary(any(), any(), any(), any())).thenReturn(new byte[]{9});
        assertThat(reports.reportCardXlsx(headmaster, 1L, 1L).getBody()).containsExactly(5);
        when(reportExportService.defaulterRows(1L)).thenReturn(List.of());
        assertThat(reports.defaulterRows()).isEmpty();
        when(reportExportService.collectionRows(any(), any(), any())).thenReturn(List.of());
        assertThat(reports.collectionRows(null, null)).isEmpty();
        when(reportExportService.meritRows(any(), any(), any())).thenReturn(List.of());
        assertThat(reports.meritRows(headmaster, 1L)).isEmpty();
        when(reportExportService.attendanceRows(any(), any(), any())).thenReturn(List.of());
        assertThat(reports.attendanceRows(java.time.LocalDate.now(), java.time.LocalDate.now())).isEmpty();
        assertThat(reports.defaulters("csv").getBody()).containsExactly(6);
        assertThat(reports.collections(null, null, "pdf").getBody()).containsExactly(7);
        assertThat(reports.meritList(headmaster, 1L, "xlsx").getBody()).containsExactly(8);
        assertThat(reports.attendance(java.time.LocalDate.now(), java.time.LocalDate.now(), "csv").getBody()).containsExactly(9);
        when(reportExportService.termResult(any(), any(), any(), any(), any(), any())).thenReturn(new byte[]{10});
        assertThat(reports.termResult(headmaster, 1L, 1L, 1L, "pdf").getBody()).containsExactly(10);
        assertThat(reports.termResult(Fixtures.principal(Role.STUDENT), null, 1L, null, "csv").getBody()).containsExactly(10);
        assertThat(reports.termResult(headmaster, null, 1L, null, "csv").getBody()).containsExactly(10);
        verify(reportExportService).termResult(eq(1L), isNull(), eq(1L), isNull(), eq(Role.HEADMASTER), eq("csv"));

        NotificationController notifications = new NotificationController(alertService, tenantResolver);
        notifications.inbox(headmaster);
        notifications.markRead(headmaster, 1L);

        DashboardController dashboard = new DashboardController(dashboardService, tenantResolver);
        dashboard.snapshot(headmaster);

        ExamController exams = new ExamController(examService, tenantResolver);
        exams.submit(1L);
        exams.verify(1L);
        exams.approve(1L);
        exams.reject(1L, "Fix marks");
        exams.generateSchedule(1L);
        exams.lockSchedule(1L, true);
        exams.seats(1L);

        GradeController grades = new GradeController(gradeService, gradeImportService, tenantResolver);
        var bulk = new tz.co.chambaka.school.management.dto.academic.BulkGradeRequest(
                1L, 1L, List.of(new tz.co.chambaka.school.management.dto.academic.BulkGradeRequest.Entry(
                        1L, BigDecimal.TEN, "Good")));
        grades.grid(1L, 1L);
        grades.bulk(teacher, bulk);
        grades.termResult(teacher, 1L, 1L, 1L, null);
        grades.classTermResults(teacher, 1L, null, 1L, 1L);

        TimetableController timetable = new TimetableController(timetableService, tenantResolver);
        timetable.generate(1L, 1L);
        when(timetableService.lock(1L, 1L, 1L, true)).thenReturn(true);
        assertThat(timetable.lock(1L, 1L, true)).containsEntry("locked", true);
        when(timetableService.isLocked(1L, 1L, 1L)).thenReturn(true);
        assertThat(timetable.lockStatus(1L, 1L)).containsEntry("locked", true);

        InvoiceController invoices = new InvoiceController(financeService, studentService, tenantResolver);
        invoices.ledger(headmaster, 1L);
        invoices.discount(headmaster, 1L,
                new tz.co.chambaka.school.management.dto.finance.InvoiceDiscountRequest(BigDecimal.ONE, "Scholarship"));

        PaymentController payments = new PaymentController(financeService, tenantResolver);
        when(financeService.receiptPdf(1L, 1L, headmaster)).thenReturn(new byte[]{3});
        assertThat(payments.receipt(headmaster, 1L).getBody()).containsExactly(3);
    }
}
