package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AssignmentRequest;
import tz.co.chambaka.school.management.dto.academic.AssignmentResponse;
import tz.co.chambaka.school.management.dto.academic.AssignmentSubmissionResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Assignment;
import tz.co.chambaka.school.management.model.AssignmentSubmission;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Teacher;
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
            AlertService alertService
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
    }

    @Transactional
    public AssignmentResponse create(Long schoolId, Long userId, AssignmentRequest request) {
        Teacher teacher = teacherService.requireByUser(userId);
        Assignment assignment = new Assignment();
        assignment.setSchoolId(schoolId);
        assignment.setTeacher(teacher);
        assignment.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
        assignment.setSection(request.sectionId() == null ? null : sectionService.require(schoolId, request.sectionId()));
        assignment.setSubject(subjectService.require(schoolId, request.subjectId()));
        assignment.setTitle(request.title());
        assignment.setInstructions(request.instructions());
        assignment.setDueDate(request.dueDate());
        assignment.setPublishedAt(Instant.now());
        Assignment saved = assignmentRepository.save(assignment);
        studentRepository.findBySchoolIdAndSchoolClassId(schoolId, request.schoolClassId()).forEach(student ->
                alertService.notifyParentsOfStudent(schoolId, student, "New assignment",
                        saved.getTitle() + " is due " + saved.getDueDate(), "ASSIGNMENT", true));
        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse attach(Long schoolId, Long id, MultipartFile file) {
        Assignment assignment = assignmentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", id));
        photoStorageService.storeStudentPhoto(schoolId, id + 900000, file);
        assignment.setAttachmentName(file.getOriginalFilename());
        assignment.setAttachmentPath("/api/v1/assignments/" + id + "/file");
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long schoolId, UserPrincipal principal) {
        List<Assignment> all = assignmentRepository.findBySchoolIdOrderByDueDateDesc(schoolId);
        if (principal.getRole() == Role.STUDENT) {
            Student me = studentService.requireByUser(principal.getId());
            return all.stream()
                    .filter(a -> a.getSchoolClass().getId().equals(me.getSchoolClass() != null ? me.getSchoolClass().getId() : -1L))
                    .map(this::toResponse).toList();
        }
        return all.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AssignmentSubmissionResponse submitWork(Long schoolId, Long userId, Long assignmentId, String notes, MultipartFile file) {
        Assignment assignment = assignmentRepository.findByIdAndSchoolId(assignmentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", assignmentId));
        Student student = studentService.requireByUser(userId);
        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElseGet(AssignmentSubmission::new);
        submission.setSchoolId(schoolId);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setNotes(notes);
        submission.setSubmittedAt(Instant.now());
        if (file != null && !file.isEmpty()) {
            photoStorageService.storeStudentPhoto(schoolId, assignmentId + student.getId() + 800000, file);
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
        assignmentRepository.findByIdAndSchoolId(assignmentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", assignmentId));
        return submissionRepository.findByAssignmentId(assignmentId).stream().map(this::toSubmission).toList();
    }

    @Transactional
    public void submitMarks(Long schoolId, Long assignmentId, Long studentId, BigDecimal marks) {
        Assignment assignment = assignmentRepository.findByIdAndSchoolId(assignmentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", assignmentId));
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
                assignment.getPublishedAt()
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
