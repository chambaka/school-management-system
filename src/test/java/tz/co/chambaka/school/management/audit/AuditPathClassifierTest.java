package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.model.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditPathClassifierTest {

    @Test
    void classifiesNoiseAuthAndScope() {
        assertThat(AuditPathClassifier.isNoise(null)).isTrue();
        assertThat(AuditPathClassifier.isNoise("/actuator/health")).isTrue();
        assertThat(AuditPathClassifier.isNoise("/swagger-ui/x")).isTrue();
        assertThat(AuditPathClassifier.isNoise("/v3/api-docs")).isTrue();
        assertThat(AuditPathClassifier.isNoise("/api/v1/public/branding/x")).isTrue();
        assertThat(AuditPathClassifier.isNoise("/api/v1/audit-events")).isTrue();
        assertThat(AuditPathClassifier.isNoise("/api/v1/platform/audit-events")).isTrue();
        assertThat(AuditPathClassifier.isNoise("/api/v1/students")).isFalse();
        assertThat(AuditPathClassifier.isAuth("/api/v1/auth/login")).isTrue();
        assertThat(AuditPathClassifier.isAuth("/api/v1/students")).isFalse();
        assertThat(AuditPathClassifier.isFinance("/api/v1/fees")).isTrue();
        assertThat(AuditPathClassifier.isFinance("/api/v1/invoices/4")).isTrue();
        assertThat(AuditPathClassifier.isFinance("/api/v1/payments")).isTrue();
        assertThat(AuditPathClassifier.isFinance("/api/v1/students")).isFalse();
        assertThat(AuditPathClassifier.isFinance(null)).isFalse();
        assertThat(AuditPathClassifier.scope("/api/v1/platform/schools", Role.ADMIN, AuditAction.UPDATE))
                .isEqualTo(AuditScope.PLATFORM);
        assertThat(AuditPathClassifier.scope("/api/v1/students", Role.SUPER_ADMIN, AuditAction.CREATE))
                .isEqualTo(AuditScope.PLATFORM);
        assertThat(AuditPathClassifier.scope("/api/v1/students", Role.ADMIN, AuditAction.REGISTER_SCHOOL))
                .isEqualTo(AuditScope.PLATFORM);
        assertThat(AuditPathClassifier.scope("/api/v1/students", Role.ADMIN, AuditAction.CREATE))
                .isEqualTo(AuditScope.TENANT);
    }

    @Test
    void actionsResourcesAndRecordingRules() {
        assertThat(AuditPathClassifier.actionFromHttp("POST", 201)).isEqualTo(AuditAction.CREATE);
        assertThat(AuditPathClassifier.actionFromHttp("PUT", 200)).isEqualTo(AuditAction.UPDATE);
        assertThat(AuditPathClassifier.actionFromHttp("PATCH", 200)).isEqualTo(AuditAction.UPDATE);
        assertThat(AuditPathClassifier.actionFromHttp("DELETE", 204)).isEqualTo(AuditAction.DELETE);
        assertThat(AuditPathClassifier.actionFromHttp("GET", 200)).isEqualTo(AuditAction.ACCESS);
        assertThat(AuditPathClassifier.actionFromHttp(null, 200)).isEqualTo(AuditAction.ACCESS);
        assertThat(AuditPathClassifier.actionFromHttp("GET", 403)).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(AuditPathClassifier.actionFromHttp("GET", 401)).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(AuditPathClassifier.actionFromHttp("GET", 500)).isEqualTo(AuditAction.ERROR);

        assertThat(AuditPathClassifier.resourceType(null)).isEqualTo("Unknown");
        assertThat(AuditPathClassifier.resourceType("/api/v1/")).isEqualTo("Unknown");
        assertThat(AuditPathClassifier.resourceType("/api/v1/students/5")).isEqualTo("Student");
        assertThat(AuditPathClassifier.resourceType("/api/v1/platform/schools/3")).isEqualTo("School");
        assertThat(AuditPathClassifier.resourceType("/api/v1/academic-years")).isEqualTo("AcademicYear");
        assertThat(AuditPathClassifier.resourceType("/api/v1/weird-things")).isEqualTo("Weird-things");
        assertThat(AuditPathClassifier.resourceId("/api/v1/students/9/parents")).isEqualTo("9");
        assertThat(AuditPathClassifier.resourceId("/api/v1/students")).isNull();
        assertThat(AuditPathClassifier.resourceId(null)).isNull();

        assertThat(AuditPathClassifier.shouldRecord("OPTIONS", "/api/v1/students", 200)).isFalse();
        assertThat(AuditPathClassifier.shouldRecord("GET", "/actuator/health", 200)).isFalse();
        assertThat(AuditPathClassifier.shouldRecord("POST", "/api/v1/auth/login", 200)).isFalse();
        assertThat(AuditPathClassifier.shouldRecord("POST", "/api/v1/auth/login", 500)).isTrue();
        assertThat(AuditPathClassifier.shouldRecord("POST", "/api/v1/students", 201)).isTrue();
        assertThat(AuditPathClassifier.shouldRecord("GET", "/api/v1/students", 200)).isFalse();
        assertThat(AuditPathClassifier.shouldRecord("GET", "/api/v1/invoices", 200)).isTrue();
        assertThat(AuditPathClassifier.shouldRecord("GET", "/api/v1/fees?academicYearId=1", 200)).isTrue();
        assertThat(AuditPathClassifier.shouldRecord("GET", "/api/v1/payments/3", 200)).isTrue();
        assertThat(AuditPathClassifier.shouldRecord("GET", "/api/v1/students", 403)).isTrue();
        assertThat(AuditPathClassifier.shouldRecord("DELETE", "/api/v1/allocations/1", 204)).isTrue();
        assertThat(AuditPathClassifier.resourceType("/api/v1/teachers")).isEqualTo("Teacher");
        assertThat(AuditPathClassifier.resourceType("/api/v1/parents")).isEqualTo("Parent");
        assertThat(AuditPathClassifier.resourceType("/api/v1/invoices")).isEqualTo("Invoice");
        assertThat(AuditPathClassifier.resourceType("/api/v1/payments")).isEqualTo("Payment");
        assertThat(AuditPathClassifier.resourceType("/api/v1/fees")).isEqualTo("FeeStructure");
        assertThat(AuditPathClassifier.resourceType("/api/v1/notices")).isEqualTo("Notice");
        assertThat(AuditPathClassifier.resourceType("/api/v1/exams")).isEqualTo("Exam");
        assertThat(AuditPathClassifier.resourceType("/api/v1/grades")).isEqualTo("Grade");
        assertThat(AuditPathClassifier.resourceType("/api/v1/attendance")).isEqualTo("Attendance");
        assertThat(AuditPathClassifier.resourceType("/api/v1/classes")).isEqualTo("SchoolClass");
        assertThat(AuditPathClassifier.resourceType("/api/v1/sections")).isEqualTo("Section");
        assertThat(AuditPathClassifier.resourceType("/api/v1/subjects")).isEqualTo("Subject");
        assertThat(AuditPathClassifier.resourceType("/api/v1/allocations")).isEqualTo("TeacherSubject");
        assertThat(AuditPathClassifier.resourceType("/api/v1/timetable")).isEqualTo("TimetableSlot");
        assertThat(AuditPathClassifier.resourceType("/api/v1/messages/inbox")).isEqualTo("StudentMessage");
        assertThat(AuditPathClassifier.resourceType("/api/v1/communications")).isEqualTo("StudentMessage");
    }
}
