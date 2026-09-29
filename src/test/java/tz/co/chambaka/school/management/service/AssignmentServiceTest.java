package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tz.co.chambaka.school.management.dto.academic.AssignmentRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Assignment;
import tz.co.chambaka.school.management.model.AssignmentSubmission;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.AssignmentStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.support.Fixtures;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock AssignmentRepository assignmentRepository;
    @Mock AssignmentSubmissionRepository submissionRepository;
    @Mock TeacherService teacherService;
    @Mock ClassService classService;
    @Mock SectionService sectionService;
    @Mock SubjectService subjectService;
    @Mock StudentService studentService;
    @Mock StudentRepository studentRepository;
    @Mock PhotoStorageService photoStorageService;
    @Mock AlertService alertService;
    @Mock AllocationService allocationService;
    @InjectMocks AssignmentService service;

    private final UserPrincipal teacher = Fixtures.principal(Role.TEACHER);
    private final UserPrincipal academic = Fixtures.principal(Role.ACADEMIC_MASTER);
    private final AssignmentRequest request = new AssignmentRequest(
            1L, 1L, 1L, "Algebra", "Questions 1-5", LocalDate.of(2026, 9, 20));

    @Test
    void createsAssignmentAsDraftWithoutAlertingParents() {
        when(teacherService.requireByUser(3L)).thenReturn(Fixtures.teacher());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var response = service.create(1L, 3L, request);

        assertThat(response.title()).isEqualTo("Algebra");
        assertThat(response.sectionName()).isEqualTo("A");
        assertThat(response.status()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(response.publishedAt()).isNull();
        verify(alertService, never()).notifyParentsOfStudent(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void createsWithoutSectionAndListsByRoleAndStatus() {
        when(teacherService.requireByUser(3L)).thenReturn(Fixtures.teacher());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        assertThat(service.create(1L, 3L, new AssignmentRequest(
                1L, null, 1L, "General", null, LocalDate.now().plusDays(2))).sectionId()).isNull();

        Assignment published = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.PUBLISHED);
        Assignment draft = assignment(3L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        var otherClass = Fixtures.schoolClass();
        otherClass.setId(2L);
        Assignment other = assignment(2L, otherClass, AssignmentStatus.PUBLISHED);
        Assignment legacy = assignment(4L, Fixtures.schoolClass(), null);
        when(assignmentRepository.findBySchoolIdOrderByDueDateDesc(1L))
                .thenReturn(List.of(published, draft, other, legacy));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT)))
                .extracting(row -> row.id())
                .containsExactly(1L, 4L);
        assertThat(service.list(1L, Fixtures.principal(Role.HEADMASTER))).hasSize(4);
        assertThat(service.list(1L, Fixtures.principal(Role.PARENT)))
                .extracting(row -> row.id())
                .containsExactly(1L, 2L, 4L);

        Student withoutClass = Fixtures.student();
        withoutClass.setSchoolClass(null);
        when(studentService.requireByUser(10L)).thenReturn(withoutClass);
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT))).isEmpty();
    }

    @Test
    void teacherUpdatesDraftAndAcademicUpdatesLocked() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        assertThat(service.update(1L, teacher, 1L, request).title()).isEqualTo("Algebra");

        Assignment locked = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.LOCKED);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(locked));
        assertThatThrownBy(() -> service.update(1L, teacher, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("academic master");

        var noYear = Fixtures.schoolClass();
        noYear.setAcademicYear(null);
        when(classService.require(1L, 1L)).thenReturn(noYear);
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        assertThat(service.update(1L, academic, 1L, new AssignmentRequest(
                1L, null, 1L, "Revised", "New notes", LocalDate.of(2026, 10, 1))).status())
                .isEqualTo(AssignmentStatus.LOCKED);
        assertThat(locked.getTitle()).isEqualTo("Revised");
        assertThat(locked.getSection()).isNull();

        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, academic, 9L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void teacherDeletesDraftAndAcademicDeletesPublished() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        when(submissionRepository.findByAssignmentId(1L)).thenReturn(List.of());
        service.delete(1L, teacher, 1L);
        verify(assignmentRepository).delete(draft);

        Assignment published = assignment(2L, Fixtures.schoolClass(), AssignmentStatus.PUBLISHED);
        AssignmentSubmission submission = new AssignmentSubmission();
        when(assignmentRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(published));
        when(submissionRepository.findByAssignmentId(2L)).thenReturn(List.of(submission));
        assertThatThrownBy(() -> service.delete(1L, teacher, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("academic master");
        service.delete(1L, academic, 2L);
        verify(submissionRepository).delete(submission);
        verify(assignmentRepository).delete(published);

        Assignment orphanClass = assignment(3L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        orphanClass.setSchoolClass(null);
        orphanClass.setSubject(null);
        when(assignmentRepository.findByIdAndSchoolId(3L, 1L)).thenReturn(Optional.of(orphanClass));
        when(submissionRepository.findByAssignmentId(3L)).thenReturn(List.of());
        service.delete(1L, teacher, 3L);
        verify(assignmentRepository).delete(orphanClass);
    }

    @Test
    void locksDraftThenPublishesAndAlertsParents() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        assertThat(service.lock(1L, teacher, 1L).status()).isEqualTo(AssignmentStatus.LOCKED);
        assertThatThrownBy(() -> service.lock(1L, teacher, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("draft");

        when(assignmentRepository.save(draft)).thenReturn(draft);
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(Fixtures.student()));
        assertThat(service.publish(1L, academic, 1L).status()).isEqualTo(AssignmentStatus.PUBLISHED);
        assertThat(draft.getPublishedAt()).isNotNull();
        verify(alertService).notifyParentsOfStudent(any(), any(), eq("New assignment"), any(), eq("ASSIGNMENT"), eq(true));
        assertThatThrownBy(() -> service.publish(1L, academic, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lock this assignment");
    }

    @Test
    void attachesFileOnDraftAndRejectsLockedTeacherEdit() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        MockMultipartFile file = new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1});
        assertThat(service.attach(1L, teacher, 1L, file).attachmentName()).isEqualTo("work.pdf");
        verify(photoStorageService).storeStudentPhoto(1L, 900001L, file);

        Assignment locked = assignment(2L, Fixtures.schoolClass(), AssignmentStatus.LOCKED);
        when(assignmentRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(locked));
        assertThatThrownBy(() -> service.attach(1L, teacher, 2L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("academic master");
        assertThat(service.attach(1L, academic, 2L, file).attachmentName()).isEqualTo("work.pdf");

        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.attach(1L, teacher, 9L, file)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void studentSubmitsWorkAndListsSubmissionsOnlyWhenPublished() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        MockMultipartFile file = new MockMultipartFile("file", "essay.pdf", "application/pdf", new byte[]{2});
        assertThatThrownBy(() -> service.submitWork(1L, 10L, 1L, "ready", file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not published");

        Assignment published = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.PUBLISHED);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(published));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        var submitted = service.submitWork(1L, 10L, 1L, "ready", file);
        assertThat(submitted.attachmentName()).isEqualTo("essay.pdf");
        verify(alertService).notifyParentsOfStudent(eq(1L), any(Student.class), eq("Assignment submitted"), any(), eq("ASSIGNMENT"), eq(false));

        AssignmentSubmission existing = new AssignmentSubmission();
        existing.setId(3L);
        existing.setAssignment(published);
        existing.setStudent(Fixtures.student());
        existing.setNotes("ready");
        existing.setSubmittedAt(Instant.now());
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.of(existing));
        service.submitWork(1L, 10L, 1L, "notes only", null);
        service.submitWork(1L, 10L, 1L, "empty", new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]));

        when(submissionRepository.findByAssignmentId(1L)).thenReturn(List.of(existing));
        assertThat(service.submissions(1L, 1L)).hasSize(1);
        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submissions(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createsAndUpdatesMarkSubmissionOnPublishedAssignment() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(75)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not published");

        Assignment published = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.PUBLISHED);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(published));
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.empty());
        service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(75));
        verify(submissionRepository).save(any(AssignmentSubmission.class));

        AssignmentSubmission existing = new AssignmentSubmission();
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.of(existing));
        service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(80));
        assertThat(existing.getMarksObtained()).isEqualByComparingTo("80");

        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submitMarks(1L, 3L, 9L, 1L, BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRequiresAllocatedTeacher() {
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        org.mockito.Mockito.doThrow(new BusinessException(
                        "Only the allocated teacher of this subject for this class can create assignments."))
                .when(allocationService)
                .requireTeachesForUser(1L, 3L, 1L, 1L, 1L, 1L, "create assignments");
        assertThatThrownBy(() -> service.create(1L, 3L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("allocated teacher");
    }

    private Assignment assignment(
            Long id,
            tz.co.chambaka.school.management.model.SchoolClass schoolClass,
            AssignmentStatus status
    ) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setTeacher(Fixtures.teacher());
        assignment.setSchoolClass(schoolClass);
        assignment.setSection(Fixtures.section());
        assignment.setSubject(Fixtures.subject());
        assignment.setTitle("Algebra");
        assignment.setInstructions("Do it");
        assignment.setDueDate(LocalDate.of(2026, 9, 20));
        assignment.setStatus(status);
        assignment.setPublishedAt(status == AssignmentStatus.PUBLISHED ? Instant.now() : null);
        return assignment;
    }
}
