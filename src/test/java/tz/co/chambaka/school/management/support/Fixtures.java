package tz.co.chambaka.school.management.support;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.dto.campus.CampusResponse;
import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.dto.tenant.TenantResponse;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.Campus;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.ExamType;
import tz.co.chambaka.school.management.model.enums.Gender;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.model.enums.TenantStatus;
import tz.co.chambaka.school.management.security.UserPrincipal;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public final class Fixtures {

    public static final Long TENANT_ID = 10L;
    public static final Long SCHOOL_ID = 1L;
    public static final Long CAMPUS_ID = 20L;

    private Fixtures() {
    }

    public static SmsProperties properties() {
        return SmsProperties.of(
                new SmsProperties.Jwt("change-this-to-a-long-random-secret-key-of-at-least-256-bits",
                        Duration.ofHours(1), Duration.ofDays(7)),
                new SmsProperties.Cors(List.of("http://localhost:3000")),
                new SmsProperties.SuperAdmin("oscar.d@example.net", "ChangeMe123!", "Platform Admin")
        );
    }

    public static User user(Long id, Role role) {
        boolean platform = role == Role.SUPER_ADMIN;
        User user = User.builder()
                .tenantId(platform ? null : TENANT_ID)
                .schoolId(platform ? null : SCHOOL_ID)
                .campusId(platform ? null : CAMPUS_ID)
                .name("User " + role)
                .email(role.name().toLowerCase() + "@example.com")
                .password("hashed")
                .role(role)
                .phone("0700000000")
                .enabled(true)
                .build();
        user.setId(id);
        return user;
    }

    public static UserPrincipal principal(Role role) {
        return new UserPrincipal(user(10L, role));
    }

    public static Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("Chambaka Group");
        tenant.setSlug("chambaka-group");
        tenant.setEmail("org@example.com");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setTimezone("Africa/Dar_es_Salaam");
        tenant.setCurrency("TZS");
        return tenant;
    }

    public static Campus campus() {
        Campus campus = new Campus();
        campus.setId(CAMPUS_ID);
        campus.setTenantId(TENANT_ID);
        campus.setSchoolId(SCHOOL_ID);
        campus.setName("Main campus");
        campus.setCode("MAIN");
        campus.setPrimaryCampus(true);
        campus.setTimezone("Africa/Dar_es_Salaam");
        return campus;
    }

    public static TenantResponse tenantResponse() {
        return new TenantResponse(
                TENANT_ID, "Chambaka Group", "chambaka-group", "org@example.com", null, "TZ",
                "Africa/Dar_es_Salaam", "TZS", TenantStatus.ACTIVE, "STARTER", null, 1);
    }

    public static CampusResponse campusResponse() {
        return new CampusResponse(CAMPUS_ID, TENANT_ID, SCHOOL_ID, "Main campus", "MAIN",
                null, null, null, "Africa/Dar_es_Salaam", true);
    }

    public static School school() {
        School school = new School();
        school.setId(SCHOOL_ID);
        school.setTenantId(TENANT_ID);
        school.setName("Chambaka Secondary");
        school.setSlug("chambaka-secondary");
        school.setEmail("school@example.com");
        school.setStatus(SchoolStatus.ACTIVE);
        school.setTimezone("Africa/Dar_es_Salaam");
        school.setCurrency("TZS");
        return school;
    }

    public static SchoolResponse schoolResponse() {
        return new SchoolResponse(
                SCHOOL_ID, TENANT_ID, "Chambaka", "chambaka", "a@b.com", null, null, null, null, null,
                "#000", "#111", "#222", null, "Africa/Dar_es_Salaam", "en", "TZS", "TZ",
                SchoolStatus.ACTIVE, "STARTER", null, true, true, true);
    }

    public static BrandingResponse branding() {
        return new BrandingResponse(SCHOOL_ID, "Chambaka", "chambaka", null, null,
                "#000", "#111", "#222", "Africa/Dar_es_Salaam", "en", "TZS", "TZ");
    }

    public static UserProfileResponse profile(User user) {
        return new UserProfileResponse(user.getId(), user.getTenantId(), user.getSchoolId(), user.getCampusId(),
                user.getName(), user.getEmail(), user.getRole(), user.getPhone(), user.getAvatarUrl(), user.isEnabled());
    }

    public static AcademicYear year() {
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setSchoolId(SCHOOL_ID);
        year.setName("2026/2027");
        year.setStartDate(LocalDate.of(2026, 1, 1));
        year.setEndDate(LocalDate.of(2026, 12, 31));
        year.setCurrentYear(true);
        return year;
    }

    public static SchoolClass schoolClass() {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(1L);
        schoolClass.setSchoolId(SCHOOL_ID);
        schoolClass.setAcademicYear(year());
        schoolClass.setName("Form 1");
        schoolClass.setCode("F1");
        schoolClass.setDisplayOrder(1);
        return schoolClass;
    }

    public static Teacher teacher() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setSchoolId(SCHOOL_ID);
        teacher.setUser(user(3L, Role.TEACHER));
        teacher.setEmployeeId("T-001");
        teacher.setDepartment("Science");
        return teacher;
    }

    public static Section section() {
        Section section = new Section();
        section.setId(1L);
        section.setSchoolId(SCHOOL_ID);
        section.setSchoolClass(schoolClass());
        section.setName("A");
        section.setCapacity(40);
        section.setClassTeacher(teacher());
        return section;
    }

    public static Student student() {
        Student student = new Student();
        student.setId(1L);
        student.setSchoolId(SCHOOL_ID);
        student.setUser(user(4L, Role.STUDENT));
        student.setAdmissionNo("ADM-001");
        student.setGender(Gender.MALE);
        student.setAcademicYear(year());
        student.setSchoolClass(schoolClass());
        student.setSection(section());
        return student;
    }

    public static Parent parent() {
        Parent parent = new Parent();
        parent.setId(1L);
        parent.setSchoolId(SCHOOL_ID);
        parent.setUser(user(5L, Role.PARENT));
        parent.setOccupation("Trader");
        return parent;
    }

    public static Subject subject() {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setSchoolId(SCHOOL_ID);
        subject.setName("Mathematics");
        subject.setCode("MATH");
        return subject;
    }

    public static Exam exam() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setSchoolId(SCHOOL_ID);
        exam.setAcademicYear(year());
        exam.setSchoolClass(schoolClass());
        exam.setName("Midterm");
        exam.setExamType(ExamType.MIDTERM);
        exam.setStartDate(LocalDate.of(2026, 3, 1));
        exam.setEndDate(LocalDate.of(2026, 3, 10));
        return exam;
    }

    public static ExamSubject examSubject() {
        ExamSubject examSubject = new ExamSubject();
        examSubject.setId(1L);
        examSubject.setSchoolId(SCHOOL_ID);
        examSubject.setExam(exam());
        examSubject.setSubject(subject());
        examSubject.setMaxMarks(new BigDecimal("100"));
        examSubject.setPassMarks(new BigDecimal("40"));
        examSubject.setExamDate(LocalDate.of(2026, 3, 2));
        return examSubject;
    }
}
