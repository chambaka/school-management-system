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
import tz.co.chambaka.school.management.model.AssignmentAttachment;
import tz.co.chambaka.school.management.model.AssignmentSubmission;
import tz.co.chambaka.school.management.model.AssignmentSubmissionAttachment;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.AssignmentStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AssignmentAttachmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionAttachmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.support.Fixtures;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock AssignmentRepository assignmentRepository;
    @Mock AssignmentAttachmentRepository attachmentRepository;
    @Mock AssignmentSubmissionRepository submissionRepository;
    @Mock AssignmentSubmissionAttachmentRepository submissionAttachmentRepository;
    @Mock TeacherService teacherService;
    @Mock ClassService classService;
    @Mock SectionService sectionService;
    @Mock SubjectService subjectService;
    @Mock StudentService studentService;
    @Mock ParentService parentService;
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
        verify(assignmentRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getStatus() == AssignmentStatus.DRAFT && saved.getPublishedAt() != null));
        verify(alertService, never()).notifyParentsOfStudent(any(), any(), any(), any(), any(), anyBoolean());
        verify(alertService, never()).notifyHouseholds(any(), any(), any(), any(), any(), anyBoolean(), any(), any(), anyBoolean());
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
        when(attachmentRepository.findByAssignment_IdInOrderByIdAsc(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(attachment(5L, published, "sheet.pdf", "application/pdf")));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        assertThat(service.list(1L, Fixtures.principal(Role.HEADMASTER)).getFirst().attachments())
                .extracting(row -> row.fileName())
                .containsExactly("sheet.pdf");
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT)))
                .extracting(row -> row.id())
                .containsExactly(1L, 4L);
        AssignmentSubmission older = new AssignmentSubmission();
        older.setId(6L);
        older.setAssignment(published);
        older.setStudent(Fixtures.student());
        older.setNotes("first try");
        older.setSubmittedAt(Instant.parse("2026-09-16T09:00:00Z"));
        AssignmentSubmission mine = new AssignmentSubmission();
        mine.setId(7L);
        mine.setAssignment(published);
        mine.setStudent(Fixtures.student());
        mine.setNotes("done");
        mine.setAttachmentName("essay.pdf");
        mine.setSubmittedAt(Instant.parse("2026-09-16T10:00:00Z"));
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of(mine, older));
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(4L, 1L))
                .thenReturn(List.of());
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT)).getFirst().mySubmission())
                .extracting(row -> row.notes(), row -> row.attachmentName(), row -> row.attempt())
                .containsExactly("done", "essay.pdf", 2);
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT)).getFirst().mySubmissionHistory())
                .extracting(row -> row.attempt(), row -> row.notes())
                .containsExactly(tuple(2, "done"), tuple(1, "first try"));
        assertThat(service.list(1L, Fixtures.principal(Role.HEADMASTER))).hasSize(4);
        when(parentService.linkedStudents(10L)).thenReturn(List.of(Fixtures.student()));
        assertThat(service.list(1L, Fixtures.principal(Role.PARENT)))
                .extracting(row -> row.id())
                .containsExactly(1L, 4L);
        assertThat(service.list(1L, Fixtures.principal(Role.PARENT)).getFirst().mySubmission())
                .extracting(row -> row.notes(), row -> row.studentName())
                .containsExactly("done", "User STUDENT");
        Student childWithoutClass = Fixtures.student();
        childWithoutClass.setSchoolClass(null);
        when(parentService.linkedStudents(10L)).thenReturn(List.of(childWithoutClass));
        assertThat(service.list(1L, Fixtures.principal(Role.PARENT))).isEmpty();

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
        AssignmentAttachment leftover = attachment(21L, draft, "gone.pdf", "application/pdf");
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L)).thenReturn(List.of(leftover));
        service.delete(1L, teacher, 1L);
        verify(photoStorageService).deleteAssignmentFile(1L, 1L, 21L);
        verify(photoStorageService).deleteLegacyAssignmentFile(1L, 1L);
        verify(attachmentRepository).deleteAll(List.of(leftover));
        verify(alertService).removeForEntity(1L, AlertService.SUBJECT_ASSIGNMENT, 1L);
        verify(assignmentRepository).delete(draft);

        Assignment published = assignment(2L, Fixtures.schoolClass(), AssignmentStatus.PUBLISHED);
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(8L);
        submission.setStudent(Fixtures.student());
        when(assignmentRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(published));
        when(submissionRepository.findByAssignmentId(2L)).thenReturn(List.of(submission));
        assertThatThrownBy(() -> service.delete(1L, teacher, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("academic master");
        service.delete(1L, academic, 2L);
        verify(photoStorageService).deleteSubmissionFile(1L, 2L, 1L);
        verify(photoStorageService).deleteSubmissionFile(1L, 2L, 1L, 8L);
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
        verify(alertService).notifyHouseholds(eq(1L), any(), eq("Assignment posted"), any(), eq("ASSIGNMENT"), eq(true),
                eq(AlertService.SUBJECT_ASSIGNMENT), eq(1L), eq(true));
        verify(alertService).notifyUser(eq(1L), eq(3L), eq("Assignment posted"), any(), eq("ASSIGNMENT"),
                eq(AlertService.SUBJECT_ASSIGNMENT), eq(1L));
        assertThatThrownBy(() -> service.publish(1L, academic, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lock this assignment");
    }

    @Test
    void attachesFileOnDraftAndRejectsLockedTeacherEdit() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        stubSaveAttachment(11L);
        draft.setAttachmentName("   ");
        MockMultipartFile file = new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1});
        var attached = service.attach(1L, teacher, 1L, file);
        assertThat(attached.attachmentName()).isEqualTo("work.pdf");
        assertThat(attached.attachments()).extracting(row -> row.fileName()).containsExactly("work.pdf");
        verify(photoStorageService).storeAssignmentFile(1L, 1L, 11L, file);

        Assignment locked = assignment(2L, Fixtures.schoolClass(), AssignmentStatus.LOCKED);
        when(assignmentRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(locked));
        assertThatThrownBy(() -> service.attach(1L, teacher, 2L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("academic master");
        stubSaveAttachment(12L);
        assertThat(service.attach(1L, academic, 2L, file).attachmentName()).isEqualTo("work.pdf");

        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.attach(1L, teacher, 9L, file)).isInstanceOf(ResourceNotFoundException.class);

        Assignment draftAgain = assignment(3L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(3L, 1L)).thenReturn(Optional.of(draftAgain));
        stubSaveAttachment(15L);
        org.mockito.Mockito.doThrow(new BusinessException("Could not save file"))
                .when(photoStorageService).storeAssignmentFile(1L, 3L, 15L, file);
        assertThatThrownBy(() -> service.attach(1L, teacher, 3L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Could not save file");
        verify(attachmentRepository).delete(org.mockito.ArgumentMatchers.any(AssignmentAttachment.class));
    }

    @Test
    void keepsZeroOrMoreDocumentsAndRemovesOne() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        when(assignmentRepository.findBySchoolIdOrderByDueDateDesc(1L)).thenReturn(List.of());
        assertThat(service.list(1L, teacher)).isEmpty();
        when(assignmentRepository.findBySchoolIdOrderByDueDateDesc(1L)).thenReturn(List.of(draft));
        assertThat(service.list(1L, teacher)).singleElement().satisfies(row -> {
            assertThat(row.attachments()).isEmpty();
            assertThat(row.attachmentName()).isNull();
        });
        draft.setAttachmentName("old.pdf");
        assertThat(service.list(1L, teacher).getFirst().attachments())
                .extracting(tz.co.chambaka.school.management.dto.academic.AssignmentAttachmentResponse::fileName)
                .containsExactly("old.pdf");
        draft.setAttachmentName(null);

        AssignmentAttachment first = attachment(11L, draft, "notes.pdf", "application/pdf");
        AssignmentAttachment second = attachment(12L, draft, "rubric.png", "image/png");
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L)).thenReturn(List.of(first, second));
        stubSaveAttachment(13L);
        MockMultipartFile extra = new MockMultipartFile("file", "extra.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{3});
        var afterAdd = service.attach(1L, teacher, 1L, extra);
        assertThat(afterAdd.attachments()).extracting(row -> row.fileName())
                .containsExactly("notes.pdf", "rubric.png", "extra.docx");

        when(attachmentRepository.findByIdAndAssignment_IdAndSchoolId(12L, 1L, 1L)).thenReturn(Optional.of(second));
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L)).thenReturn(List.of(first));
        var afterRemove = service.removeFile(1L, teacher, 1L, 12L);
        verify(photoStorageService).deleteAssignmentFile(1L, 1L, 12L);
        verify(photoStorageService, never()).deleteLegacyAssignmentFile(1L, 1L);
        assertThat(afterRemove.attachments()).extracting(row -> row.fileName()).containsExactly("notes.pdf");
        assertThat(afterRemove.attachmentName()).isEqualTo("notes.pdf");

        when(attachmentRepository.findByIdAndAssignment_IdAndSchoolId(11L, 1L, 1L)).thenReturn(Optional.of(first));
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L)).thenReturn(List.of());
        var empty = service.removeFile(1L, teacher, 1L, 11L);
        verify(photoStorageService).deleteLegacyAssignmentFile(1L, 1L);
        assertThat(empty.attachments()).isEmpty();
        assertThat(empty.attachmentName()).isNull();

        Assignment locked = assignment(2L, Fixtures.schoolClass(), AssignmentStatus.LOCKED);
        when(assignmentRepository.findByIdAndSchoolId(2L, 1L)).thenReturn(Optional.of(locked));
        assertThatThrownBy(() -> service.removeFile(1L, teacher, 2L, 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("academic master");
        when(attachmentRepository.findByIdAndAssignment_IdAndSchoolId(9L, 2L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeFile(1L, academic, 2L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adoptsLegacySingleFileThenAddsAnotherDocument() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        draft.setAttachmentName("old.pdf");
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        stubSaveAttachment(14L);
        MockMultipartFile firstExtra = new MockMultipartFile("file", "only-new.png", "image/png", new byte[]{4});
        assertThat(service.attach(1L, teacher, 1L, firstExtra).attachments())
                .extracting(row -> row.fileName())
                .containsExactly("only-new.png");

        when(photoStorageService.findAssignmentFile(1L, 1L))
                .thenReturn(Optional.of(new StoredPhoto(Path.of("1.pdf"), "application/pdf")));
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(attachment(10L, draft, "old.pdf", "application/pdf")));
        java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(10);
        when(attachmentRepository.saveAndFlush(any(AssignmentAttachment.class))).thenAnswer(invocation -> {
            AssignmentAttachment saved = invocation.getArgument(0);
            saved.setId(ids.getAndIncrement());
            return saved;
        });
        MockMultipartFile extra = new MockMultipartFile("file", "new.png", "image/png", new byte[]{4});
        var response = service.attach(1L, teacher, 1L, extra);
        assertThat(response.attachments()).extracting(row -> row.fileName()).contains("old.pdf", "new.png");
    }

    @Test
    void servesAssignmentAndSubmissionFilesByRole() {
        Assignment draft = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.DRAFT);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(draft));
        StoredPhoto stored = new StoredPhoto(Path.of("1.pdf"), "application/pdf");
        when(photoStorageService.findAssignmentFile(1L, 1L)).thenReturn(Optional.of(stored));
        assertThat(service.file(1L, teacher, 1L)).isEqualTo(stored);
        assertThatThrownBy(() -> service.file(1L, Fixtures.principal(Role.STUDENT), 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        Assignment published = assignment(1L, Fixtures.schoolClass(), AssignmentStatus.PUBLISHED);
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(published));
        assertThat(service.file(1L, Fixtures.principal(Role.PARENT), 1L)).isEqualTo(stored);
        assertThat(service.file(1L, Fixtures.principal(Role.STUDENT), 1L)).isEqualTo(stored);
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L))
                .thenReturn(List.of(attachment(11L, published, "a.pdf", "application/pdf")));
        when(photoStorageService.findAssignmentFile(1L, 1L, 11L)).thenReturn(Optional.of(stored));
        assertThat(service.file(1L, teacher, 1L)).isEqualTo(stored);
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L)).thenReturn(List.of());
        when(photoStorageService.findAssignmentFile(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.file(1L, teacher, 1L)).isInstanceOf(ResourceNotFoundException.class);

        when(attachmentRepository.findByIdAndAssignment_IdAndSchoolId(11L, 1L, 1L))
                .thenReturn(Optional.of(attachment(11L, published, "a.pdf", "application/pdf")));
        when(photoStorageService.findAssignmentFile(1L, 1L, 11L)).thenReturn(Optional.of(stored));
        assertThat(service.file(1L, teacher, 1L, 11L)).isEqualTo(stored);
        when(attachmentRepository.findByIdAndAssignment_IdAndSchoolId(9L, 1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.file(1L, teacher, 1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(attachmentRepository.findByAssignment_IdOrderByIdAsc(1L))
                .thenReturn(List.of(attachment(11L, published, "a.pdf", "application/pdf")));
        when(photoStorageService.findAssignmentFile(1L, 1L, 11L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.file(1L, teacher, 1L)).isInstanceOf(ResourceNotFoundException.class);

        when(photoStorageService.findSubmissionFile(1L, 1L, 1L)).thenReturn(Optional.of(stored));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        assertThat(service.submissionFile(1L, Fixtures.principal(Role.STUDENT), 1L, 1L)).isEqualTo(stored);
        assertThatThrownBy(() -> service.submissionFile(1L, Fixtures.principal(Role.STUDENT), 1L, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not allowed");
        assertThat(service.submissionFile(1L, academic, 1L, 1L)).isEqualTo(stored);
        assertThat(service.submissionFile(1L, Fixtures.principal(Role.PARENT), 1L, 1L)).isEqualTo(stored);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Student is not linked to this parent"))
                .when(parentService).assertLinked(10L, 9L);
        assertThatThrownBy(() -> service.submissionFile(1L, Fixtures.principal(Role.PARENT), 1L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
        when(photoStorageService.findSubmissionFile(1L, 1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submissionFile(1L, academic, 1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        AssignmentSubmission attempt = new AssignmentSubmission();
        attempt.setId(3L);
        attempt.setSchoolId(1L);
        attempt.setAssignment(published);
        attempt.setStudent(Fixtures.student());
        when(submissionRepository.findByIdAndAssignment_IdAndStudent_IdAndSchoolId(3L, 1L, 1L, 1L))
                .thenReturn(Optional.of(attempt));
        when(photoStorageService.findSubmissionFile(1L, 1L, 1L, 3L)).thenReturn(Optional.of(stored));
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of(attempt));
        assertThat(service.submissionFile(1L, academic, 1L, 1L)).isEqualTo(stored);
        assertThat(service.submissionFile(1L, academic, 1L, 1L, 3L)).isEqualTo(stored);
        when(photoStorageService.findSubmissionFile(1L, 1L, 1L, 3L, 12L)).thenReturn(Optional.of(stored));
        when(submissionAttachmentRepository.findByIdAndSubmission_IdAndSchoolId(12L, 3L, 1L))
                .thenReturn(Optional.of(new AssignmentSubmissionAttachment()));
        assertThat(service.submissionFile(1L, academic, 1L, 1L, 3L, 12L)).isEqualTo(stored);
        when(submissionAttachmentRepository.findByIdAndSubmission_IdAndSchoolId(9L, 3L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submissionFile(1L, academic, 1L, 1L, 3L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
        when(submissionRepository.findByIdAndAssignment_IdAndStudent_IdAndSchoolId(9L, 1L, 1L, 1L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submissionFile(1L, academic, 1L, 1L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
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
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of());
        org.mockito.stubbing.Answer<AssignmentSubmission> persist = inv -> {
            AssignmentSubmission saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(saved.getNotes() != null && saved.getNotes().contains("notes") ? 4L : 3L);
            }
            return saved;
        };
        when(submissionRepository.saveAndFlush(any(AssignmentSubmission.class))).thenAnswer(persist);
        when(submissionAttachmentRepository.saveAndFlush(any(AssignmentSubmissionAttachment.class))).thenAnswer(inv -> {
            AssignmentSubmissionAttachment saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        var submitted = service.submitWork(1L, 10L, 1L, "ready", file);
        assertThat(submitted.attachmentName()).isEqualTo("essay.pdf");
        assertThat(submitted.attempt()).isEqualTo(1);
        verify(photoStorageService).storeSubmissionFile(1L, 1L, 1L, 3L, 11L, file);
        verify(alertService).notifyParentsOfStudent(eq(1L), any(Student.class), eq("Assignment submitted"), any(), eq("ASSIGNMENT"), eq(false),
                eq(AlertService.SUBJECT_ASSIGNMENT), eq(1L), eq(true));
        verify(alertService).notifyUser(eq(1L), eq(3L), eq("Assignment submitted"), any(), eq("ASSIGNMENT"),
                eq(AlertService.SUBJECT_ASSIGNMENT), eq(1L));

        AssignmentSubmission existing = new AssignmentSubmission();
        existing.setId(3L);
        existing.setSchoolId(1L);
        existing.setAssignment(published);
        existing.setStudent(Fixtures.student());
        existing.setNotes("ready");
        existing.setSubmittedAt(Instant.parse("2026-09-16T10:00:00Z"));
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of(existing));
        var second = service.submitWork(1L, 10L, 1L, "notes only", null);
        assertThat(second.attempt()).isEqualTo(2);
        assertThat(existing.getNotes()).isEqualTo("ready");
        service.submitWork(1L, 10L, 1L, "empty", new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]));

        when(submissionRepository.findByAssignmentIdOrderBySubmittedAtDescIdDesc(1L)).thenReturn(List.of(existing));
        assertThat(service.submissions(1L, 1L)).hasSize(1);
        when(parentService.linkedStudents(10L)).thenReturn(List.of(Fixtures.student()));
        assertThat(service.submissions(1L, Fixtures.principal(Role.PARENT), 1L)).hasSize(1);
        Student otherChild = Fixtures.student();
        otherChild.setId(9L);
        when(parentService.linkedStudents(10L)).thenReturn(List.of(otherChild));
        assertThat(service.submissions(1L, Fixtures.principal(Role.PARENT), 1L)).isEmpty();
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        assertThat(service.submissions(1L, Fixtures.principal(Role.STUDENT), 1L)).hasSize(1);
        existing.setNotes("ready");
        when(submissionRepository.findMine(1L)).thenReturn(List.of(existing));
        assertThat(service.mySubmissions(1L, 10L))
                .extracting(row -> row.notes(), row -> row.assignmentId(), row -> row.attempt())
                .containsExactly(tuple("ready", 1L, 1));
        assertThat(service.mySubmission(1L, Fixtures.principal(Role.STUDENT), 1L).notes()).isEqualTo("ready");
        AssignmentSubmissionAttachment extra = new AssignmentSubmissionAttachment();
        extra.setId(12L);
        extra.setFileName("scan.pdf");
        extra.setContentType("application/pdf");
        extra.setSubmission(existing);
        when(submissionAttachmentRepository.saveAndFlush(any(AssignmentSubmissionAttachment.class))).thenAnswer(inv -> {
            AssignmentSubmissionAttachment saved = inv.getArgument(0);
            saved.setId(12L);
            return saved;
        });
        when(submissionAttachmentRepository.findBySubmission_IdOrderByIdAsc(3L)).thenReturn(List.of(extra));
        when(submissionAttachmentRepository.findBySubmission_IdInOrderByIdAsc(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(extra));
        assertThat(service.attachMyFile(1L, Fixtures.principal(Role.STUDENT), 1L, file).attachments())
                .extracting(row -> row.fileName())
                .contains("scan.pdf");
        assertThat(existing.getNotes()).isEqualTo("ready");
        existing.setSchoolId(2L);
        assertThat(service.mySubmissions(1L, 10L)).isEmpty();
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
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of());
        service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(75));
        verify(submissionRepository).save(any(AssignmentSubmission.class));

        AssignmentSubmission existing = new AssignmentSubmission();
        existing.setNotes("kept");
        when(submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of(existing));
        service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(80));
        assertThat(existing.getMarksObtained()).isEqualByComparingTo("80");
        assertThat(existing.getNotes()).isEqualTo("kept");

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

    private void stubSaveAttachment(Long id) {
        when(attachmentRepository.saveAndFlush(any(AssignmentAttachment.class))).thenAnswer(invocation -> {
            AssignmentAttachment saved = invocation.getArgument(0);
            saved.setId(id);
            return saved;
        });
    }

    private AssignmentAttachment attachment(Long id, Assignment assignment, String fileName, String contentType) {
        AssignmentAttachment row = new AssignmentAttachment();
        row.setId(id);
        row.setSchoolId(1L);
        row.setAssignment(assignment);
        row.setFileName(fileName);
        row.setContentType(contentType);
        return row;
    }
}
