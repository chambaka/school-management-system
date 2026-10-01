package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AssignmentAttachmentResponse;
import tz.co.chambaka.school.management.dto.academic.AssignmentRequest;
import tz.co.chambaka.school.management.dto.academic.AssignmentResponse;
import tz.co.chambaka.school.management.dto.academic.AssignmentSubmissionResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Assignment;
import tz.co.chambaka.school.management.model.AssignmentAttachment;
import tz.co.chambaka.school.management.model.AssignmentSubmission;
import tz.co.chambaka.school.management.model.AssignmentSubmissionAttachment;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.enums.AssignmentStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AssignmentAttachmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionAttachmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository attachmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentSubmissionAttachmentRepository submissionAttachmentRepository;
    private final TeacherService teacherService;
    private final ClassService classService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final ParentService parentService;
    private final StudentRepository studentRepository;
    private final PhotoStorageService photoStorageService;
    private final AlertService alertService;
    private final AllocationService allocationService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentAttachmentRepository attachmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            AssignmentSubmissionAttachmentRepository submissionAttachmentRepository,
            TeacherService teacherService,
            ClassService classService,
            SectionService sectionService,
            SubjectService subjectService,
            StudentService studentService,
            ParentService parentService,
            StudentRepository studentRepository,
            PhotoStorageService photoStorageService,
            AlertService alertService,
            AllocationService allocationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.attachmentRepository = attachmentRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.teacherService = teacherService;
        this.classService = classService;
        this.sectionService = sectionService;
        this.subjectService = subjectService;
        this.studentService = studentService;
        this.parentService = parentService;
        this.studentRepository = studentRepository;
        this.photoStorageService = photoStorageService;
        this.alertService = alertService;
        this.allocationService = allocationService;
    }

    @Transactional
    public AssignmentResponse create(Long schoolId, Long userId, AssignmentRequest request) {
        SchoolClass schoolClass = classService.require(schoolId, request.schoolClassId());
        assertTeachesAssignment(
                schoolId, userId, schoolClass, request.subjectId(), request.sectionId(),
                "create assignments");
        Teacher teacher = teacherService.requireByUser(userId);
        Assignment assignment = new Assignment();
        assignment.setSchoolId(schoolId);
        assignment.setTeacher(teacher);
        applyDetails(schoolId, assignment, schoolClass, request);
        assignment.setStatus(AssignmentStatus.DRAFT);
        assignment.setPublishedAt(Instant.now());
        Assignment saved = assignmentRepository.save(assignment);
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse update(Long schoolId, UserPrincipal principal, Long id, AssignmentRequest request) {
        Assignment assignment = require(schoolId, id);
        assertMutable(principal, assignment);
        SchoolClass schoolClass = classService.require(schoolId, request.schoolClassId());
        if (!principal.getRole().managesAssignments()) {
            assertTeachesAssignment(
                    schoolId, principal.getId(), schoolClass, request.subjectId(), request.sectionId(),
                    "edit this assignment");
        }
        applyDetails(schoolId, assignment, schoolClass, request);
        return toResponse(assignment);
    }

    @Transactional
    public void delete(Long schoolId, UserPrincipal principal, Long id) {
        Assignment assignment = require(schoolId, id);
        assertMutable(principal, assignment);
        if (!principal.getRole().managesAssignments()) {
            assertTeachesExisting(schoolId, principal.getId(), assignment, "delete this assignment");
        }
        List<AssignmentAttachment> files = attachmentRepository.findByAssignment_IdOrderByIdAsc(id);
        files.forEach(file -> photoStorageService.deleteAssignmentFile(schoolId, id, file.getId()));
        photoStorageService.deleteLegacyAssignmentFile(schoolId, id);
        attachmentRepository.deleteAll(files);
        submissionRepository.findByAssignmentId(id).forEach(submission -> {
            Long studentId = submission.getStudent() == null ? null : submission.getStudent().getId();
            if (studentId != null) {
                photoStorageService.deleteSubmissionFile(schoolId, id, studentId);
                if (submission.getId() != null) {
                    photoStorageService.deleteSubmissionFile(schoolId, id, studentId, submission.getId());
                    submissionFilesOf(submission.getId()).forEach(file ->
                            photoStorageService.deleteSubmissionFile(
                                    schoolId, id, studentId, submission.getId(), file.getId()));
                }
            }
            if (submission.getId() != null) {
                submissionAttachmentRepository.deleteAll(submissionFilesOf(submission.getId()));
            }
            submissionRepository.delete(submission);
        });
        alertService.removeForEntity(schoolId, AlertService.SUBJECT_ASSIGNMENT, id);
        assignmentRepository.delete(assignment);
    }

    @Transactional
    public AssignmentResponse lock(Long schoolId, UserPrincipal principal, Long id) {
        Assignment assignment = require(schoolId, id);
        assertCanManageWorkflow(schoolId, principal, assignment, "lock this assignment");
        if (statusOf(assignment) != AssignmentStatus.DRAFT) {
            throw new BusinessException("Only a draft assignment can be locked.");
        }
        assignment.setStatus(AssignmentStatus.LOCKED);
        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse publish(Long schoolId, UserPrincipal principal, Long id) {
        Assignment assignment = require(schoolId, id);
        assertCanManageWorkflow(schoolId, principal, assignment, "publish this assignment");
        if (statusOf(assignment) != AssignmentStatus.LOCKED) {
            throw new BusinessException("Lock this assignment before publishing it.");
        }
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setPublishedAt(Instant.now());
        Assignment saved = assignmentRepository.save(assignment);
        String title = "Assignment posted";
        String body = saved.getTitle() + " is due " + saved.getDueDate();
        alertService.notifyHouseholds(
                schoolId,
                audienceOf(schoolId, saved),
                title,
                body,
                "ASSIGNMENT",
                true,
                AlertService.SUBJECT_ASSIGNMENT,
                saved.getId(),
                true);
        if (saved.getTeacher() != null && saved.getTeacher().getUser() != null) {
            alertService.notifyUser(
                    schoolId,
                    saved.getTeacher().getUser().getId(),
                    title,
                    body,
                    "ASSIGNMENT",
                    AlertService.SUBJECT_ASSIGNMENT,
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse attach(Long schoolId, UserPrincipal principal, Long id, MultipartFile file) {
        Assignment assignment = requireMutableAssignment(schoolId, principal, id, "create assignments");
        adoptLegacyAttachment(schoolId, assignment);
        AssignmentAttachment row = new AssignmentAttachment();
        row.setSchoolId(schoolId);
        row.setAssignment(assignment);
        row.setFileName(file.getOriginalFilename());
        row.setContentType(file.getContentType());
        AssignmentAttachment saved = attachmentRepository.saveAndFlush(row);
        try {
            photoStorageService.storeAssignmentFile(schoolId, id, saved.getId(), file);
        } catch (RuntimeException ex) {
            attachmentRepository.delete(saved);
            throw ex;
        }
        List<AssignmentAttachment> files = attachmentsOf(id);
        if (files.stream().noneMatch(existing -> saved.getId().equals(existing.getId()))) {
            files = new ArrayList<>(files);
            files.add(saved);
        }
        syncPrimaryAttachment(assignment, files);
        return toResponse(assignment, files);
    }

    @Transactional
    public AssignmentResponse removeFile(Long schoolId, UserPrincipal principal, Long id, Long fileId) {
        Assignment assignment = requireMutableAssignment(schoolId, principal, id, "delete this assignment");
        AssignmentAttachment row = requireAttachment(schoolId, id, fileId);
        photoStorageService.deleteAssignmentFile(schoolId, id, fileId);
        attachmentRepository.delete(row);
        List<AssignmentAttachment> remaining = attachmentsOf(id);
        if (remaining.isEmpty()) {
            photoStorageService.deleteLegacyAssignmentFile(schoolId, id);
        }
        syncPrimaryAttachment(assignment, remaining);
        return toResponse(assignment, remaining);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long schoolId, UserPrincipal principal) {
        List<Assignment> all = assignmentRepository.findBySchoolIdOrderByDueDateDesc(schoolId);
        Map<Long, List<AssignmentAttachment>> files = attachmentsByAssignment(all);
        if (principal.getRole() == Role.STUDENT) {
            Student me = studentService.requireByUser(principal.getId());
            Long classId = me.getSchoolClass() != null ? me.getSchoolClass().getId() : -1L;
            List<Assignment> visible = all.stream()
                    .filter(assignment -> statusOf(assignment) == AssignmentStatus.PUBLISHED)
                    .filter(assignment -> assignment.getSchoolClass().getId().equals(classId))
                    .toList();
            Map<Long, List<AssignmentSubmission>> mine = submissionsByAssignment(me.getId(), visible);
            return visible.stream()
                    .map(assignment -> toResponse(
                            assignment,
                            files.getOrDefault(assignment.getId(), List.of()),
                            mine.getOrDefault(assignment.getId(), List.of())))
                    .toList();
        }
        if (principal.getRole() == Role.PARENT) {
            List<Student> children = parentService.linkedStudents(principal.getId());
            List<Assignment> visible = all.stream()
                    .filter(assignment -> statusOf(assignment) == AssignmentStatus.PUBLISHED)
                    .filter(assignment -> children.stream().anyMatch(child -> visibleToChild(assignment, child)))
                    .toList();
            Map<Long, List<AssignmentSubmission>> work = new HashMap<>();
            for (Student child : children) {
                for (Map.Entry<Long, List<AssignmentSubmission>> entry : submissionsByAssignment(child.getId(), visible).entrySet()) {
                    work.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
                }
            }
            return visible.stream()
                    .map(assignment -> toResponse(
                            assignment,
                            files.getOrDefault(assignment.getId(), List.of()),
                            work.getOrDefault(assignment.getId(), List.of())))
                    .toList();
        }
        return all.stream()
                .map(assignment -> toResponse(assignment, files.getOrDefault(assignment.getId(), List.of())))
                .toList();
    }

    @Transactional
    public AssignmentSubmissionResponse submitWork(Long schoolId, Long userId, Long assignmentId, String notes, MultipartFile file) {
        Assignment assignment = require(schoolId, assignmentId);
        requirePublished(assignment);
        Student student = studentService.requireByUser(userId);
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setSchoolId(schoolId);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setNotes(notes);
        submission.setSubmittedAt(Instant.now());
        AssignmentSubmission saved = submissionRepository.saveAndFlush(submission);
        if (file != null && !file.isEmpty()) {
            addSubmissionFile(schoolId, saved, file);
        }
        String body = student.getUser().getName() + " submitted " + assignment.getTitle();
        alertService.notifyParentsOfStudent(schoolId, student, "Assignment submitted",
                body, "ASSIGNMENT", false,
                AlertService.SUBJECT_ASSIGNMENT, assignmentId, true);
        if (assignment.getTeacher() != null && assignment.getTeacher().getUser() != null) {
            alertService.notifyUser(
                    schoolId,
                    assignment.getTeacher().getUser().getId(),
                    "Assignment submitted",
                    body,
                    "ASSIGNMENT",
                    AlertService.SUBJECT_ASSIGNMENT,
                    assignmentId);
        }
        List<AssignmentSubmission> attempts = new ArrayList<>(assignmentAttempts(assignmentId, student.getId()));
        if (attempts.stream().noneMatch(row -> saved.getId() != null && saved.getId().equals(row.getId()))) {
            attempts.add(saved);
        }
        return latestOf(attempts);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> submissions(Long schoolId, Long assignmentId) {
        return submissions(schoolId, null, assignmentId);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> submissions(Long schoolId, UserPrincipal principal, Long assignmentId) {
        if (principal == null) {
            require(schoolId, assignmentId);
        } else {
            requireVisibleAssignment(schoolId, principal, assignmentId);
        }
        List<AssignmentSubmission> rows = submissionRepository.findByAssignmentIdOrderBySubmittedAtDescIdDesc(assignmentId);
        if (principal != null && principal.getRole() == Role.PARENT) {
            Set<Long> children = linkedStudentIds(principal.getId());
            rows = rows.stream()
                    .filter(row -> row.getStudent() != null && children.contains(row.getStudent().getId()))
                    .toList();
        } else if (principal != null && principal.getRole() == Role.STUDENT) {
            Long me = studentService.requireByUser(principal.getId()).getId();
            rows = rows.stream()
                    .filter(row -> row.getStudent() != null && me.equals(row.getStudent().getId()))
                    .toList();
        }
        return toSubmissionHistory(rows);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> mySubmissions(Long schoolId, Long userId) {
        Student me = studentService.requireByUser(userId);
        return toSubmissionHistory(submissionRepository.findMine(me.getId()).stream()
                .filter(row -> schoolId.equals(row.getSchoolId()))
                .toList());
    }

    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse mySubmission(Long schoolId, UserPrincipal principal, Long assignmentId) {
        requireVisibleAssignment(schoolId, principal, assignmentId);
        Student me = studentService.requireByUser(principal.getId());
        return latestOf(assignmentAttempts(assignmentId, me.getId()));
    }

    @Transactional
    public AssignmentSubmissionResponse attachMyFile(Long schoolId, UserPrincipal principal, Long assignmentId, MultipartFile file) {
        Assignment assignment = requireVisibleAssignment(schoolId, principal, assignmentId);
        requirePublished(assignment);
        Student student = studentService.requireByUser(principal.getId());
        AssignmentSubmission submission = latestAttempt(assignmentId, student.getId());
        if (submission == null) {
            AssignmentSubmission created = new AssignmentSubmission();
            created.setSchoolId(schoolId);
            created.setAssignment(assignment);
            created.setStudent(student);
            created.setSubmittedAt(Instant.now());
            submission = submissionRepository.saveAndFlush(created);
        }
        AssignmentSubmission current = submission;
        addSubmissionFile(schoolId, current, file);
        List<AssignmentSubmission> attempts = new ArrayList<>(assignmentAttempts(assignmentId, student.getId()));
        if (attempts.stream().noneMatch(row -> current.getId() != null && current.getId().equals(row.getId()))) {
            attempts.add(current);
        }
        return latestOf(attempts);
    }

    @Transactional(readOnly = true)
    public StoredPhoto file(Long schoolId, UserPrincipal principal, Long id) {
        requireVisibleAssignment(schoolId, principal, id);
        List<AssignmentAttachment> files = attachmentsOf(id);
        if (!files.isEmpty()) {
            return resolveStoredFile(schoolId, id, files.getFirst().getId());
        }
        return photoStorageService.findAssignmentFile(schoolId, id)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", id));
    }

    @Transactional(readOnly = true)
    public StoredPhoto file(Long schoolId, UserPrincipal principal, Long id, Long fileId) {
        requireVisibleAssignment(schoolId, principal, id);
        requireAttachment(schoolId, id, fileId);
        return resolveStoredFile(schoolId, id, fileId);
    }

    @Transactional(readOnly = true)
    public StoredPhoto submissionFile(Long schoolId, UserPrincipal principal, Long assignmentId, Long studentId) {
        return submissionFile(schoolId, principal, assignmentId, studentId, null, null);
    }

    @Transactional(readOnly = true)
    public StoredPhoto submissionFile(
            Long schoolId,
            UserPrincipal principal,
            Long assignmentId,
            Long studentId,
            Long submissionId
    ) {
        return submissionFile(schoolId, principal, assignmentId, studentId, submissionId, null);
    }

    @Transactional(readOnly = true)
    public StoredPhoto submissionFile(
            Long schoolId,
            UserPrincipal principal,
            Long assignmentId,
            Long studentId,
            Long submissionId,
            Long fileId
    ) {
        require(schoolId, assignmentId);
        if (principal.getRole() == Role.STUDENT) {
            Long me = studentService.requireByUser(principal.getId()).getId();
            if (!me.equals(studentId)) {
                throw new BusinessException("Not allowed to view this file");
            }
        } else if (principal.getRole() == Role.PARENT) {
            parentService.assertLinked(principal.getId(), studentId);
        } else if (principal.getRole() != Role.HEADMASTER
                && principal.getRole() != Role.ACADEMIC_MASTER
                && principal.getRole() != Role.TEACHER
                && principal.getRole() != Role.SCHOOL_ADMIN) {
            throw new BusinessException("Not allowed to view this file");
        }
        AssignmentSubmission row = submissionId == null
                ? latestAttempt(assignmentId, studentId)
                : submissionRepository.findByIdAndAssignment_IdAndStudent_IdAndSchoolId(
                        submissionId, assignmentId, studentId, schoolId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", submissionId));
        return resolveSubmissionStored(schoolId, assignmentId, studentId, row, fileId);
    }

    @Transactional
    public void submitMarks(Long schoolId, Long userId, Long assignmentId, Long studentId, BigDecimal marks) {
        Assignment assignment = require(schoolId, assignmentId);
        requirePublished(assignment);
        assertTeachesExisting(schoolId, userId, assignment, "submit marks");
        Student student = studentService.require(schoolId, studentId);
        AssignmentSubmission submission = latestAttempt(assignmentId, studentId);
        if (submission == null) {
            submission = new AssignmentSubmission();
            submission.setSchoolId(schoolId);
            submission.setAssignment(assignment);
            submission.setStudent(student);
            submission.setSubmittedAt(Instant.now());
        }
        submission.setMarksObtained(marks);
        submissionRepository.save(submission);
    }

    private void applyDetails(Long schoolId, Assignment assignment, SchoolClass schoolClass, AssignmentRequest request) {
        assignment.setSchoolClass(schoolClass);
        assignment.setSection(request.sectionId() == null ? null : sectionService.require(schoolId, request.sectionId()));
        assignment.setSubject(subjectService.require(schoolId, request.subjectId()));
        assignment.setTitle(request.title());
        assignment.setInstructions(request.instructions());
        assignment.setDueDate(request.dueDate());
    }

    private Assignment require(Long schoolId, Long id) {
        return assignmentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", id));
    }

    private List<Student> audienceOf(Long schoolId, Assignment assignment) {
        List<Student> students = studentRepository.findBySchoolIdAndSchoolClassId(
                schoolId, assignment.getSchoolClass().getId());
        if (assignment.getSection() == null || assignment.getSection().getId() == null) {
            return students;
        }
        Long sectionId = assignment.getSection().getId();
        return students.stream()
                .filter(student -> student.getSection() != null && sectionId.equals(student.getSection().getId()))
                .toList();
    }

    private boolean visibleToChild(Assignment assignment, Student student) {
        if (student == null || student.getSchoolClass() == null || assignment.getSchoolClass() == null) {
            return false;
        }
        if (!student.getSchoolClass().getId().equals(assignment.getSchoolClass().getId())) {
            return false;
        }
        if (assignment.getSection() == null || assignment.getSection().getId() == null) {
            return true;
        }
        return student.getSection() != null && assignment.getSection().getId().equals(student.getSection().getId());
    }

    private Set<Long> linkedStudentIds(Long userId) {
        return parentService.linkedStudents(userId).stream()
                .map(Student::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Assignment requireMutableAssignment(Long schoolId, UserPrincipal principal, Long id, String action) {
        Assignment assignment = require(schoolId, id);
        assertMutable(principal, assignment);
        if (!principal.getRole().managesAssignments()) {
            assertTeachesExisting(schoolId, principal.getId(), assignment, action);
        }
        return assignment;
    }

    private Assignment requireVisibleAssignment(Long schoolId, UserPrincipal principal, Long id) {
        Assignment assignment = require(schoolId, id);
        if ((principal.getRole() == Role.STUDENT || principal.getRole() == Role.PARENT)
                && statusOf(assignment) != AssignmentStatus.PUBLISHED) {
            throw ResourceNotFoundException.of("Assignment", id);
        }
        return assignment;
    }

    private AssignmentAttachment requireAttachment(Long schoolId, Long assignmentId, Long fileId) {
        return attachmentRepository.findByIdAndAssignment_IdAndSchoolId(fileId, assignmentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", fileId));
    }

    private StoredPhoto resolveStoredFile(Long schoolId, Long assignmentId, Long fileId) {
        return photoStorageService.findAssignmentFile(schoolId, assignmentId, fileId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", fileId));
    }

    private List<AssignmentAttachment> attachmentsOf(Long assignmentId) {
        return attachmentRepository.findByAssignment_IdOrderByIdAsc(assignmentId);
    }

    private Map<Long, List<AssignmentSubmission>> submissionsByAssignment(Long studentId, List<Assignment> assignments) {
        Map<Long, List<AssignmentSubmission>> mine = new HashMap<>();
        for (Assignment assignment : assignments) {
            mine.put(assignment.getId(), assignmentAttempts(assignment.getId(), studentId));
        }
        return mine;
    }

    private List<AssignmentSubmission> assignmentAttempts(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentIdOrderBySubmittedAtDescIdDesc(assignmentId, studentId);
    }

    private AssignmentSubmission latestAttempt(Long assignmentId, Long studentId) {
        List<AssignmentSubmission> rows = assignmentAttempts(assignmentId, studentId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private List<AssignmentSubmissionAttachment> submissionFilesOf(Long submissionId) {
        return submissionAttachmentRepository.findBySubmission_IdOrderByIdAsc(submissionId);
    }

    private Map<Long, List<AssignmentSubmissionAttachment>> submissionFilesBySubmission(List<AssignmentSubmission> rows) {
        List<Long> ids = rows.stream().map(AssignmentSubmission::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return submissionAttachmentRepository.findBySubmission_IdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(row -> row.getSubmission().getId()));
    }

    private void addSubmissionFile(Long schoolId, AssignmentSubmission submission, MultipartFile file) {
        AssignmentSubmissionAttachment row = new AssignmentSubmissionAttachment();
        row.setSchoolId(schoolId);
        row.setSubmission(submission);
        row.setFileName(file.getOriginalFilename());
        row.setContentType(file.getContentType());
        AssignmentSubmissionAttachment saved = submissionAttachmentRepository.saveAndFlush(row);
        try {
            photoStorageService.storeSubmissionFile(
                    schoolId,
                    submission.getAssignment().getId(),
                    submission.getStudent().getId(),
                    submission.getId(),
                    saved.getId(),
                    file);
        } catch (RuntimeException ex) {
            submissionAttachmentRepository.delete(saved);
            throw ex;
        }
        List<AssignmentSubmissionAttachment> files = new ArrayList<>(submissionFilesOf(submission.getId()));
        if (files.stream().noneMatch(existing -> saved.getId().equals(existing.getId()))) {
            files.add(saved);
        }
        syncSubmissionPrimary(submission, files);
    }

    private void syncSubmissionPrimary(AssignmentSubmission submission, List<AssignmentSubmissionAttachment> files) {
        if (files.isEmpty()) {
            return;
        }
        AssignmentSubmissionAttachment first = files.getFirst();
        submission.setAttachmentName(first.getFileName());
        submission.setAttachmentPath("/api/v1/assignments/" + submission.getAssignment().getId()
                + "/submissions/" + submission.getStudent().getId()
                + "/attempts/" + submission.getId()
                + "/files/" + first.getId());
    }

    private StoredPhoto resolveSubmissionStored(
            Long schoolId,
            Long assignmentId,
            Long studentId,
            AssignmentSubmission row,
            Long fileId
    ) {
        if (fileId != null) {
            if (row == null || row.getId() == null) {
                throw ResourceNotFoundException.of("Assignment file", fileId);
            }
            submissionAttachmentRepository.findByIdAndSubmission_IdAndSchoolId(fileId, row.getId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", fileId));
            return photoStorageService.findSubmissionFile(schoolId, assignmentId, studentId, row.getId(), fileId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", fileId));
        }
        if (row != null && row.getId() != null) {
            List<AssignmentSubmissionAttachment> files = submissionFilesOf(row.getId());
            if (!files.isEmpty()) {
                return photoStorageService.findSubmissionFile(
                                schoolId, assignmentId, studentId, row.getId(), files.getFirst().getId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", assignmentId));
            }
            return photoStorageService.findSubmissionFile(schoolId, assignmentId, studentId, row.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", assignmentId));
        }
        return photoStorageService.findSubmissionFile(schoolId, assignmentId, studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", assignmentId));
    }

    private Map<Long, List<AssignmentAttachment>> attachmentsByAssignment(List<Assignment> assignments) {
        List<Long> ids = assignments.stream().map(Assignment::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return attachmentRepository.findByAssignment_IdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(row -> row.getAssignment().getId()));
    }

    private void adoptLegacyAttachment(Long schoolId, Assignment assignment) {
        if (!attachmentsOf(assignment.getId()).isEmpty()) {
            return;
        }
        if (assignment.getAttachmentName() == null || assignment.getAttachmentName().isBlank()) {
            return;
        }
        StoredPhoto stored = photoStorageService.findAssignmentFile(schoolId, assignment.getId()).orElse(null);
        if (stored == null) {
            return;
        }
        AssignmentAttachment row = new AssignmentAttachment();
        row.setSchoolId(schoolId);
        row.setAssignment(assignment);
        row.setFileName(assignment.getAttachmentName());
        row.setContentType(stored.contentType());
        attachmentRepository.saveAndFlush(row);
    }

    private void syncPrimaryAttachment(Assignment assignment, List<AssignmentAttachment> files) {
        if (files.isEmpty()) {
            assignment.setAttachmentName(null);
            assignment.setAttachmentPath(null);
            return;
        }
        AssignmentAttachment first = files.getFirst();
        assignment.setAttachmentName(first.getFileName());
        assignment.setAttachmentPath("/api/v1/assignments/" + assignment.getId() + "/files/" + first.getId());
    }

    private void requirePublished(Assignment assignment) {
        if (statusOf(assignment) != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("This assignment is not published yet.");
        }
    }

    private void assertMutable(UserPrincipal principal, Assignment assignment) {
        if (statusOf(assignment) != AssignmentStatus.DRAFT && !principal.getRole().managesAssignments()) {
            throw new BusinessException("Only the academic master can edit or delete a locked or published assignment.");
        }
    }

    private void assertCanManageWorkflow(Long schoolId, UserPrincipal principal, Assignment assignment, String action) {
        if (!principal.getRole().managesAssignments()) {
            assertTeachesExisting(schoolId, principal.getId(), assignment, action);
        }
    }

    private void assertTeachesExisting(Long schoolId, Long userId, Assignment assignment, String action) {
        Long sectionId = assignment.getSection() == null ? null : assignment.getSection().getId();
        Long subjectId = assignment.getSubject() == null ? null : assignment.getSubject().getId();
        assertTeachesAssignment(schoolId, userId, assignment.getSchoolClass(), subjectId, sectionId, action);
    }

    private void assertTeachesAssignment(
            Long schoolId,
            Long userId,
            SchoolClass schoolClass,
            Long subjectId,
            Long sectionId,
            String action
    ) {
        Long classId = schoolClass == null ? null : schoolClass.getId();
        Long yearId = schoolClass == null || schoolClass.getAcademicYear() == null
                ? null
                : schoolClass.getAcademicYear().getId();
        allocationService.requireTeachesForUser(schoolId, userId, yearId, classId, subjectId, sectionId, action);
    }

    private AssignmentStatus statusOf(Assignment assignment) {
        return assignment.getStatus() == null ? AssignmentStatus.PUBLISHED : assignment.getStatus();
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return toResponse(assignment, attachmentsOf(assignment.getId()), List.of());
    }

    private AssignmentResponse toResponse(Assignment assignment, List<AssignmentAttachment> files) {
        return toResponse(assignment, files, List.of());
    }

    private AssignmentResponse toResponse(
            Assignment assignment,
            List<AssignmentAttachment> files,
            List<AssignmentSubmission> submissions
    ) {
        List<AssignmentAttachmentResponse> attachments = toAttachmentResponses(assignment, files);
        String attachmentName = attachments.isEmpty() ? null : attachments.getFirst().fileName();
        List<AssignmentSubmissionResponse> history = toSubmissionHistory(submissions);
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTeacher().getId(),
                assignment.getTeacher().getUser().getName(),
                assignment.getSchoolClass().getId(),
                assignment.getSchoolClass().getName(),
                assignment.getSection() != null ? assignment.getSection().getId() : null,
                assignment.getSection() != null ? assignment.getSection().getName() : null,
                assignment.getSubject().getId(),
                assignment.getSubject().getName(),
                assignment.getTitle(),
                assignment.getInstructions(),
                assignment.getDueDate(),
                attachmentName,
                attachments,
                statusOf(assignment),
                statusOf(assignment) == AssignmentStatus.PUBLISHED ? assignment.getPublishedAt() : null,
                history.isEmpty() ? null : history.getFirst(),
                history
        );
    }

    private List<AssignmentAttachmentResponse> toAttachmentResponses(
            Assignment assignment,
            List<AssignmentAttachment> files
    ) {
        if (!files.isEmpty()) {
            return files.stream()
                    .map(file -> new AssignmentAttachmentResponse(file.getId(), file.getFileName(), file.getContentType()))
                    .toList();
        }
        if (assignment.getAttachmentName() != null && !assignment.getAttachmentName().isBlank()) {
            return List.of(new AssignmentAttachmentResponse(null, assignment.getAttachmentName(), null));
        }
        return List.of();
    }

    private AssignmentSubmissionResponse latestOf(List<AssignmentSubmission> rows) {
        List<AssignmentSubmissionResponse> history = toSubmissionHistory(rows);
        return history.isEmpty() ? null : history.getFirst();
    }

    private List<AssignmentSubmissionResponse> toSubmissionHistory(List<AssignmentSubmission> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AssignmentSubmissionAttachment>> files = submissionFilesBySubmission(rows);
        Map<Long, List<AssignmentSubmission>> byStudent = new LinkedHashMap<>();
        List<AssignmentSubmission> unknown = new ArrayList<>();
        for (AssignmentSubmission row : rows) {
            Long studentId = row.getStudent() == null ? null : row.getStudent().getId();
            if (studentId == null) {
                unknown.add(row);
                continue;
            }
            byStudent.computeIfAbsent(studentId, key -> new ArrayList<>()).add(row);
        }
        List<AssignmentSubmissionResponse> history = new ArrayList<>();
        for (List<AssignmentSubmission> group : byStudent.values()) {
            history.addAll(numberedAttempts(group, files));
        }
        if (!unknown.isEmpty()) {
            history.addAll(numberedAttempts(unknown, files));
        }
        history.sort(Comparator
                .comparing(AssignmentSubmissionResponse::submittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AssignmentSubmissionResponse::id, Comparator.nullsLast(Comparator.reverseOrder())));
        return history;
    }

    private List<AssignmentSubmissionResponse> numberedAttempts(
            List<AssignmentSubmission> group,
            Map<Long, List<AssignmentSubmissionAttachment>> files
    ) {
        List<AssignmentSubmission> oldestFirst = group.stream().sorted(ATTEMPT_ORDER).toList();
        List<AssignmentSubmissionResponse> history = new ArrayList<>();
        for (int index = 0; index < oldestFirst.size(); index++) {
            AssignmentSubmission row = oldestFirst.get(index);
            history.add(toSubmission(row, index + 1, files.getOrDefault(row.getId(), List.of())));
        }
        return history;
    }

    private AssignmentSubmissionResponse toSubmission(
            AssignmentSubmission submission,
            int attempt,
            List<AssignmentSubmissionAttachment> files
    ) {
        List<AssignmentAttachmentResponse> attachments = files.stream()
                .map(file -> new AssignmentAttachmentResponse(file.getId(), file.getFileName(), file.getContentType()))
                .toList();
        if (attachments.isEmpty() && submission.getAttachmentName() != null && !submission.getAttachmentName().isBlank()) {
            attachments = List.of(new AssignmentAttachmentResponse(null, submission.getAttachmentName(), null));
        }
        String attachmentName = attachments.isEmpty() ? submission.getAttachmentName() : attachments.getFirst().fileName();
        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getUser().getName(),
                submission.getNotes(),
                attachmentName,
                submission.getMarksObtained(),
                submission.getSubmittedAt(),
                attempt,
                attachments
        );
    }

    private static final Comparator<AssignmentSubmission> ATTEMPT_ORDER = Comparator
            .comparing(AssignmentSubmission::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AssignmentSubmission::getId, Comparator.nullsLast(Comparator.naturalOrder()));
}
