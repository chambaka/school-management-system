package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AssignmentRequest;
import tz.co.chambaka.school.management.dto.academic.AssignmentResponse;
import tz.co.chambaka.school.management.dto.academic.AssignmentSubmissionResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Assignment;
import tz.co.chambaka.school.management.model.AssignmentSubmission;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.enums.AssignmentStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final TeacherService teacherService;
    private final ClassService classService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final PhotoStorageService photoStorageService;
    private final AlertService alertService;
    private final AllocationService allocationService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            TeacherService teacherService,
            ClassService classService,
            SectionService sectionService,
            SubjectService subjectService,
            StudentService studentService,
            StudentRepository studentRepository,
            PhotoStorageService photoStorageService,
            AlertService alertService,
            AllocationService allocationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.teacherService = teacherService;
        this.classService = classService;
        this.sectionService = sectionService;
        this.subjectService = subjectService;
        this.studentService = studentService;
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
        submissionRepository.findByAssignmentId(id).forEach(submissionRepository::delete);
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
        studentRepository.findBySchoolIdAndSchoolClassId(schoolId, saved.getSchoolClass().getId()).forEach(student ->
                alertService.notifyParentsOfStudent(schoolId, student, "New assignment",
                        saved.getTitle() + " is due " + saved.getDueDate(), "ASSIGNMENT", true));
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse attach(Long schoolId, UserPrincipal principal, Long id, MultipartFile file) {
        Assignment assignment = require(schoolId, id);
        assertMutable(principal, assignment);
        if (!principal.getRole().managesAssignments()) {
            assertTeachesExisting(schoolId, principal.getId(), assignment, "create assignments");
        }
        photoStorageService.storeAssignmentFile(schoolId, id, file);
        assignment.setAttachmentName(file.getOriginalFilename());
        assignment.setAttachmentPath("/api/v1/assignments/" + id + "/file");
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long schoolId, UserPrincipal principal) {
        List<Assignment> all = assignmentRepository.findBySchoolIdOrderByDueDateDesc(schoolId);
        if (principal.getRole() == Role.STUDENT) {
            Student me = studentService.requireByUser(principal.getId());
            Long classId = me.getSchoolClass() != null ? me.getSchoolClass().getId() : -1L;
            return all.stream()
                    .filter(assignment -> statusOf(assignment) == AssignmentStatus.PUBLISHED)
                    .filter(assignment -> assignment.getSchoolClass().getId().equals(classId))
                    .map(this::toResponse)
                    .toList();
        }
        if (principal.getRole() == Role.PARENT) {
            return all.stream()
                    .filter(assignment -> statusOf(assignment) == AssignmentStatus.PUBLISHED)
                    .map(this::toResponse)
                    .toList();
        }
        return all.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AssignmentSubmissionResponse submitWork(Long schoolId, Long userId, Long assignmentId, String notes, MultipartFile file) {
        Assignment assignment = require(schoolId, assignmentId);
        requirePublished(assignment);
        Student student = studentService.requireByUser(userId);
        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseGet(AssignmentSubmission::new);
        submission.setSchoolId(schoolId);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setNotes(notes);
        submission.setSubmittedAt(Instant.now());
        if (file != null && !file.isEmpty()) {
            photoStorageService.storeSubmissionFile(schoolId, assignmentId, student.getId(), file);
            submission.setAttachmentName(file.getOriginalFilename());
            submission.setAttachmentPath("/api/v1/assignments/" + assignmentId + "/submissions/" + student.getId() + "/file");
        }
        AssignmentSubmission saved = submissionRepository.save(submission);
        alertService.notifyParentsOfStudent(schoolId, student, "Assignment submitted",
                student.getUser().getName() + " submitted " + assignment.getTitle(), "ASSIGNMENT", false);
        return toSubmission(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> submissions(Long schoolId, Long assignmentId) {
        require(schoolId, assignmentId);
        return submissionRepository.findByAssignmentId(assignmentId).stream().map(this::toSubmission).toList();
    }

    @Transactional(readOnly = true)
    public StoredPhoto file(Long schoolId, UserPrincipal principal, Long id) {
        Assignment assignment = require(schoolId, id);
        if ((principal.getRole() == Role.STUDENT || principal.getRole() == Role.PARENT)
                && statusOf(assignment) != AssignmentStatus.PUBLISHED) {
            throw ResourceNotFoundException.of("Assignment", id);
        }
        return photoStorageService.findAssignmentFile(schoolId, id)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", id));
    }

    @Transactional(readOnly = true)
    public StoredPhoto submissionFile(Long schoolId, UserPrincipal principal, Long assignmentId, Long studentId) {
        require(schoolId, assignmentId);
        if (principal.getRole() == Role.STUDENT) {
            Long me = studentService.requireByUser(principal.getId()).getId();
            if (!me.equals(studentId)) {
                throw new BusinessException("Not allowed to view this file");
            }
        } else if (principal.getRole() != Role.HEADMASTER
                && principal.getRole() != Role.ACADEMIC_MASTER
                && principal.getRole() != Role.TEACHER
                && principal.getRole() != Role.SCHOOL_ADMIN) {
            throw new BusinessException("Not allowed to view this file");
        }
        return photoStorageService.findSubmissionFile(schoolId, assignmentId, studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment file", assignmentId));
    }

    @Transactional
    public void submitMarks(Long schoolId, Long userId, Long assignmentId, Long studentId, BigDecimal marks) {
        Assignment assignment = require(schoolId, assignmentId);
        requirePublished(assignment);
        assertTeachesExisting(schoolId, userId, assignment, "submit marks");
        Student student = studentService.require(schoolId, studentId);
        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(AssignmentSubmission::new);
        submission.setSchoolId(schoolId);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setMarksObtained(marks);
        submission.setSubmittedAt(Instant.now());
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
                assignment.getAttachmentName(),
                statusOf(assignment),
                statusOf(assignment) == AssignmentStatus.PUBLISHED ? assignment.getPublishedAt() : null
        );
    }

    private AssignmentSubmissionResponse toSubmission(AssignmentSubmission submission) {
        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getUser().getName(),
                submission.getNotes(),
                submission.getAttachmentName(),
                submission.getMarksObtained(),
                submission.getSubmittedAt()
        );
    }
}
