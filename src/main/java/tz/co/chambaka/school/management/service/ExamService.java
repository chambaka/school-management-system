package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamResponse;
import tz.co.chambaka.school.management.dto.academic.ExamSeatResponse;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSeat;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.model.enums.AssessmentComponent;
import tz.co.chambaka.school.management.model.enums.ExamApprovalStatus;
import tz.co.chambaka.school.management.model.enums.ExamType;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ExamSeatRepository;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import tz.co.chambaka.school.management.solver.ExamConflictEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamSeatRepository examSeatRepository;
    private final AcademicYearService academicYearService;
    private final AcademicTermService academicTermService;
    private final ClassService classService;
    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AlertService alertService;
    private final GradeRepository gradeRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;

    public ExamService(
            ExamRepository examRepository,
            ExamSubjectRepository examSubjectRepository,
            ExamSeatRepository examSeatRepository,
            AcademicYearService academicYearService,
            AcademicTermService academicTermService,
            ClassService classService,
            SubjectService subjectService,
            TeacherService teacherService,
            ClassroomRepository classroomRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            AlertService alertService,
            GradeRepository gradeRepository,
            TeacherSubjectRepository teacherSubjectRepository
    ) {
        this.examRepository = examRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.examSeatRepository = examSeatRepository;
        this.academicYearService = academicYearService;
        this.academicTermService = academicTermService;
        this.classService = classService;
        this.subjectService = subjectService;
        this.teacherService = teacherService;
        this.classroomRepository = classroomRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.alertService = alertService;
        this.gradeRepository = gradeRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> list(Long schoolId, Long academicYearId) {
        return list(schoolId, academicYearId, Role.HEADMASTER);
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> list(Long schoolId, Long academicYearId, Role role) {
        List<Exam> exams;
        boolean publicOnly = role == Role.PARENT || role == Role.STUDENT;
        if (publicOnly) {
            exams = academicYearId == null
                    ? examRepository.findBySchoolIdAndPublishedTrueOrderByStartDateDesc(schoolId)
                    : examRepository.findBySchoolIdAndAcademicYearIdAndPublishedTrueOrderByStartDateDesc(schoolId, academicYearId);
        } else {
            exams = academicYearId == null
                    ? examRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                    : examRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateDesc(schoolId, academicYearId);
        }
        return enrich(schoolId, exams);
    }

    @Transactional
    public ExamResponse create(Long schoolId, ExamRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Exam end date must be after start date");
        }
        Exam exam = new Exam();
        exam.setSchoolId(schoolId);
        exam.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        exam.setAcademicTerm(request.academicTermId() == null ? null : academicTermService.require(schoolId, request.academicTermId()));
        exam.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
        exam.setName(request.name());
        exam.setExamType(request.examType());
        exam.setAssessmentComponent(request.assessmentComponent() != null
                ? request.assessmentComponent()
                : defaultComponent(request.examType()));
        exam.setApprovalStatus(ExamApprovalStatus.DRAFT);
        exam.setStartDate(request.startDate());
        exam.setEndDate(request.endDate());
        Exam saved = examRepository.save(exam);
        log.info("Created exam id={} schoolId={} name={}", saved.getId(), schoolId, saved.getName());
        return toExam(saved);
    }

    @Transactional
    public ExamResponse update(Long schoolId, Long id, ExamRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Exam end date must be after start date");
        }
        Exam exam = require(schoolId, id);
        exam.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        exam.setAcademicTerm(request.academicTermId() == null ? null : academicTermService.require(schoolId, request.academicTermId()));
        exam.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
        exam.setName(request.name());
        exam.setExamType(request.examType());
        exam.setAssessmentComponent(request.assessmentComponent() != null
                ? request.assessmentComponent()
                : defaultComponent(request.examType()));
        exam.setStartDate(request.startDate());
        exam.setEndDate(request.endDate());
        return toExam(exam);
    }

    @Transactional
    public ExamResponse submit(Long schoolId, Long id) {
        Exam exam = require(schoolId, id);
        if (exam.getApprovalStatus() != ExamApprovalStatus.DRAFT
                && exam.getApprovalStatus() != ExamApprovalStatus.ENTERED
                && exam.getApprovalStatus() != ExamApprovalStatus.REJECTED) {
            throw new BusinessException("Marks can only be submitted while the exam is in draft, entered, or rejected");
        }
        if (!gradeRepository.existsByExamId(id)) {
            throw new BusinessException("Enter marks before sending this exam for checking");
        }
        exam.setApprovalStatus(ExamApprovalStatus.ENTERED);
        exam.setRejectionNote(null);
        return toExam(exam);
    }

    @Transactional
    public ExamResponse reject(Long schoolId, Long id, String note) {
        Exam exam = require(schoolId, id);
        ExamApprovalStatus status = exam.getApprovalStatus();
        if (status != ExamApprovalStatus.ENTERED
                && status != ExamApprovalStatus.VERIFIED
                && status != ExamApprovalStatus.APPROVED
                && status != ExamApprovalStatus.PUBLISHED) {
            throw new BusinessException("Only submitted results can be returned to the teacher");
        }
        exam.setApprovalStatus(ExamApprovalStatus.REJECTED);
        exam.setRejectionNote(note == null || note.isBlank() ? "Returned to the teacher" : note.trim());
        exam.setVerifiedAt(null);
        exam.setApprovedAt(null);
        exam.setPublished(false);
        return toExam(exam);
    }

    @Transactional
    public ExamResponse verify(Long schoolId, Long id) {
        Exam exam = require(schoolId, id);
        if (exam.getApprovalStatus() != ExamApprovalStatus.ENTERED) {
            throw new BusinessException("Academic Master can only verify entered marks");
        }
        exam.setApprovalStatus(ExamApprovalStatus.VERIFIED);
        exam.setVerifiedAt(Instant.now());
        return toExam(exam);
    }

    @Transactional
    public ExamResponse approve(Long schoolId, Long id) {
        Exam exam = require(schoolId, id);
        if (exam.getApprovalStatus() != ExamApprovalStatus.VERIFIED) {
            throw new BusinessException("Only verified results can be approved");
        }
        exam.setApprovalStatus(ExamApprovalStatus.APPROVED);
        exam.setApprovedAt(Instant.now());
        return toExam(exam);
    }

    @Transactional
    public ExamResponse publish(Long schoolId, Long id, boolean published) {
        Exam exam = require(schoolId, id);
        if (published) {
            if (exam.getApprovalStatus() != ExamApprovalStatus.APPROVED && exam.getApprovalStatus() != ExamApprovalStatus.PUBLISHED) {
                throw new BusinessException("Results must be approved before publication");
            }
            exam.setApprovalStatus(ExamApprovalStatus.PUBLISHED);
            exam.setPublished(true);
            exam.setPublishedAt(Instant.now());
            studentRepository.findBySchoolIdAndSchoolClassId(schoolId, exam.getSchoolClass().getId()).forEach(student ->
                    alertService.notifyParentsOfStudent(schoolId, student,
                            "Results published",
                            exam.getName() + " results are now available.",
                            "RESULTS", true));
        } else {
            exam.setPublished(false);
            if (exam.getApprovalStatus() == ExamApprovalStatus.PUBLISHED) {
                exam.setApprovalStatus(ExamApprovalStatus.APPROVED);
            }
        }
        log.info("Set exam published={} examId={} schoolId={}", published, id, schoolId);
        return toExam(exam);
    }

    public void assertMarksEditable(Exam exam) {
        ExamApprovalStatus status = exam.getApprovalStatus() == null ? ExamApprovalStatus.DRAFT : exam.getApprovalStatus();
        if (status != ExamApprovalStatus.DRAFT
                && status != ExamApprovalStatus.ENTERED
                && status != ExamApprovalStatus.REJECTED) {
            throw new BusinessException("Marks are frozen after Academic Master verification");
        }
    }

    public void assertVisibleTo(Exam exam, Role role) {
        if ((role == Role.PARENT || role == Role.STUDENT) && !exam.isPublished()) {
            throw new ResourceNotFoundException("Results are not yet published");
        }
    }

    @Transactional
    public ExamSubjectResponse addSubject(Long schoolId, Long examId, ExamSubjectRequest request) {
        Exam exam = require(schoolId, examId);
        if (exam.isScheduleLocked()) {
            throw new BusinessException("This exam timetable is locked");
        }
        if (examSubjectRepository.existsByExamIdAndSubjectId(examId, request.subjectId())) {
            throw new DuplicateResourceException("Subject is already added to this exam");
        }
        if (request.passMarks().compareTo(request.maxMarks()) > 0) {
            throw new BusinessException("Pass marks cannot exceed max marks");
        }
        requireInvigilator(request);
        ExamSubject examSubject = new ExamSubject();
        examSubject.setSchoolId(schoolId);
        examSubject.setExam(exam);
        examSubject.setSubject(subjectService.require(schoolId, request.subjectId()));
        examSubject.setMaxMarks(request.maxMarks());
        examSubject.setPassMarks(request.passMarks());
        examSubject.setExamDate(request.examDate());
        examSubject.setStartTime(request.startTime());
        examSubject.setEndTime(request.endTime());
        examSubject.setVenue(request.venue());
        examSubject.setInvigilator(teacherService.require(schoolId, request.invigilatorId()));
        return toExamSubject(examSubjectRepository.save(examSubject));
    }

    @Transactional
    public ExamSubjectResponse updateSubject(Long schoolId, Long examId, Long paperId, ExamSubjectRequest request) {
        Exam exam = require(schoolId, examId);
        ExamSubject paper = requirePaper(schoolId, examId, paperId);
        assertPaperWritable(exam);
        if (request.passMarks().compareTo(request.maxMarks()) > 0) {
            throw new BusinessException("Pass marks cannot exceed max marks");
        }
        Long currentSubjectId = paper.getSubject().getId();
        if (!currentSubjectId.equals(request.subjectId())) {
            if (gradeRepository.existsByExamSubjectId(paper.getId())) {
                throw new BusinessException("Remove marks for this paper first");
            }
            if (examSubjectRepository.existsByExamIdAndSubjectId(examId, request.subjectId())) {
                throw new DuplicateResourceException("Subject is already added to this exam");
            }
            paper.setSubject(subjectService.require(schoolId, request.subjectId()));
        }
        applyPaper(schoolId, paper, request);
        return toExamSubject(examSubjectRepository.save(paper));
    }

    @Transactional
    public void deleteSubject(Long schoolId, Long examId, Long paperId) {
        Exam exam = require(schoolId, examId);
        ExamSubject paper = requirePaper(schoolId, examId, paperId);
        if (exam.isPublished()) {
            throw new BusinessException("Unpublish this exam first");
        }
        if (gradeRepository.existsByExamSubjectId(paper.getId())) {
            throw new BusinessException("Remove marks for this paper first");
        }
        examSeatRepository.deleteByExamSubjectId(paper.getId());
        examSubjectRepository.delete(paper);
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectResponse> listSubjects(Long schoolId, Long examId) {
        require(schoolId, examId);
        return examSubjectRepository.findByExamId(examId).stream().map(this::toExamSubject).toList();
    }

    @Transactional
    public List<ExamSubjectResponse> generateSchedule(Long schoolId, Long examId) {
        Exam exam = require(schoolId, examId);
        if (exam.isScheduleLocked()) {
            throw new BusinessException("This exam timetable is locked");
        }
        List<ExamSubject> papers = examSubjectRepository.findByExamId(examId);
        List<Classroom> rooms = classroomRepository.findBySchoolIdOrderByNameAsc(schoolId);
        List<Teacher> teachers = teacherRepository.findAllBySchoolId(schoolId);
        LocalDate date = exam.getStartDate();
        boolean morning = true;
        int teacherIndex = 0;
        List<ExamSubject> placed = new ArrayList<>();
        for (ExamSubject paper : papers) {
            List<Student> students = studentRepository.findBySchoolIdAndSchoolClassId(schoolId, exam.getSchoolClass().getId())
                    .stream().filter(s -> s.getStatus() == null || s.getStatus() == StudentStatus.ACTIVE).toList();
            int safety = 0;
            while (safety++ < 20) {
                paper.setExamDate(date);
                paper.setStartTime(morning ? LocalTime.of(8, 0) : LocalTime.of(11, 0));
                paper.setEndTime(morning ? LocalTime.of(10, 0) : LocalTime.of(12, 30));
                Classroom room = rooms.isEmpty() ? null : rooms.get(Math.min(teacherIndex, rooms.size() - 1));
                if (room != null && ExamConflictEngine.roomTooSmall(room, students.size())) {
                    room = rooms.stream()
                            .filter(candidate -> !ExamConflictEngine.roomTooSmall(candidate, students.size()))
                            .findFirst()
                            .orElse(room);
                }
                paper.setVenue(room == null ? "Hall A" : room.getName());
                if (!teachers.isEmpty()) {
                    paper.setInvigilator(teachers.get(teacherIndex % teachers.size()));
                }
                if (!ExamConflictEngine.invigilatorClash(paper, placed) && !ExamConflictEngine.venueClash(paper, placed)) {
                    break;
                }
                teacherIndex++;
                if (!morning) {
                    date = date.plusDays(1);
                    if (date.getDayOfWeek().getValue() > 5) {
                        date = date.plusDays(8L - date.getDayOfWeek().getValue());
                    }
                }
                morning = !morning;
                if (date.isAfter(exam.getEndDate())) {
                    date = exam.getEndDate();
                }
            }
            placed.add(paper);
            examSeatRepository.deleteByExamSubjectId(paper.getId());
            int seat = 1;
            for (Student student : students) {
                ExamSeat examSeat = new ExamSeat();
                examSeat.setSchoolId(schoolId);
                examSeat.setExamSubject(paper);
                examSeat.setStudent(student);
                examSeat.setSeatNumber(String.format("S-%02d", seat++));
                examSeatRepository.save(examSeat);
            }
            if (!morning) {
                date = date.plusDays(1);
                if (date.getDayOfWeek().getValue() > 5) {
                    date = date.plusDays(8L - date.getDayOfWeek().getValue());
                }
            }
            morning = !morning;
            if (date.isAfter(exam.getEndDate())) {
                date = exam.getEndDate();
            }
        }
        return papers.stream().map(this::toExamSubject).toList();
    }

    @Transactional
    public ExamResponse lockSchedule(Long schoolId, Long examId, boolean locked) {
        Exam exam = require(schoolId, examId);
        if (locked && !hasSchedule(examSubjectRepository.findByExamId(examId))) {
            throw new BusinessException("Schedule the papers before locking");
        }
        exam.setScheduleLocked(locked);
        return toExam(exam);
    }

    @Transactional(readOnly = true)
    public List<ExamSeatResponse> seats(Long schoolId, Long examSubjectId) {
        return examSeatRepository.findByExamSubjectIdOrderBySeatNumberAsc(examSubjectId).stream()
                .map(seat -> new ExamSeatResponse(
                        seat.getId(),
                        seat.getExamSubject().getId(),
                        seat.getStudent().getId(),
                        seat.getStudent().getUser().getName(),
                        seat.getStudent().getAdmissionNo(),
                        seat.getSeatNumber()))
                .toList();
    }

    public Exam require(Long schoolId, Long id) {
        return examRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", id));
    }

    private ExamSubject requirePaper(Long schoolId, Long examId, Long paperId) {
        ExamSubject paper = examSubjectRepository.findByIdAndSchoolId(paperId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam paper", paperId));
        if (!paper.getExam().getId().equals(examId)) {
            throw new ResourceNotFoundException("Exam paper does not belong to this exam");
        }
        return paper;
    }

    private void assertPaperWritable(Exam exam) {
        if (exam.isScheduleLocked()) {
            throw new BusinessException("This exam timetable is locked");
        }
    }

    private void applyPaper(Long schoolId, ExamSubject paper, ExamSubjectRequest request) {
        paper.setMaxMarks(request.maxMarks());
        paper.setPassMarks(request.passMarks());
        paper.setExamDate(request.examDate());
        paper.setStartTime(request.startTime());
        paper.setEndTime(request.endTime());
        paper.setVenue(request.venue());
        requireInvigilator(request);
        paper.setInvigilator(teacherService.require(schoolId, request.invigilatorId()));
    }

    private void requireInvigilator(ExamSubjectRequest request) {
        if (request.invigilatorId() == null) {
            throw new BusinessException("Choose an invigilator for this paper");
        }
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        Exam exam = require(schoolId, id);
        if (exam.isPublished()) {
            throw new BusinessException("Unpublish this exam first");
        }
        if (gradeRepository.existsByExamId(id)) {
            throw new BusinessException("Remove marks for this exam first");
        }
        for (ExamSubject paper : examSubjectRepository.findByExamId(id)) {
            examSeatRepository.deleteByExamSubjectId(paper.getId());
        }
        examSubjectRepository.deleteByExamId(id);
        examRepository.delete(exam);
    }

    private static AssessmentComponent defaultComponent(ExamType type) {
        return switch (type) {
            case MIDTERM -> AssessmentComponent.MIDTERM;
            case SEMI_TERMINAL -> AssessmentComponent.SEMI_TERMINAL;
            case FINAL -> AssessmentComponent.TERMINAL;
            default -> AssessmentComponent.OTHER;
        };
    }

    private List<ExamResponse> enrich(Long schoolId, List<Exam> exams) {
        if (exams.isEmpty()) {
            return List.of();
        }
        List<Long> ids = exams.stream().map(Exam::getId).toList();
        Map<Long, List<ExamSubject>> papersByExam = examSubjectRepository.findByExamIdIn(ids).stream()
                .collect(Collectors.groupingBy(paper -> paper.getExam().getId()));
        List<TeacherSubject> allocations = teacherSubjectRepository.findBySchoolId(schoolId);
        return exams.stream()
                .map(exam -> toExam(exam, papersByExam.getOrDefault(exam.getId(), List.of()), allocations))
                .toList();
    }

    private ExamResponse toExam(Exam exam) {
        return toExam(exam, List.of(), List.of());
    }

    private ExamResponse toExam(
            Exam exam,
            List<ExamSubject> papers,
            List<TeacherSubject> allocations
    ) {
        List<String> subjects = papers.stream()
                .map(paper -> paper.getSubject().getName())
                .distinct()
                .toList();
        LinkedHashSet<Long> subjectIds = papers.stream()
                .map(paper -> paper.getSubject().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> teachers = allocations.stream()
                .filter(allocation -> allocation.getAcademicYear().getId().equals(exam.getAcademicYear().getId())
                        && allocation.getSchoolClass().getId().equals(exam.getSchoolClass().getId())
                        && (subjectIds.isEmpty() || subjectIds.contains(allocation.getSubject().getId())))
                .map(allocation -> allocation.getTeacher().getUser().getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (teachers.isEmpty()) {
            papers.stream()
                    .map(ExamSubject::getInvigilator)
                    .filter(Objects::nonNull)
                    .map(teacher -> teacher.getUser().getName())
                    .forEach(teachers::add);
        }
        return new ExamResponse(
                exam.getId(),
                exam.getAcademicYear().getId(),
                exam.getAcademicYear().getName(),
                exam.getAcademicYear().isCurrentYear(),
                exam.getAcademicTerm() != null ? exam.getAcademicTerm().getId() : null,
                exam.getAcademicTerm() != null ? exam.getAcademicTerm().getName() : null,
                exam.getSchoolClass().getId(),
                exam.getSchoolClass().getName(),
                exam.getName(),
                exam.getExamType(),
                exam.getAssessmentComponent() == null ? AssessmentComponent.OTHER : exam.getAssessmentComponent(),
                exam.getApprovalStatus() == null ? ExamApprovalStatus.DRAFT : exam.getApprovalStatus(),
                exam.getStartDate(),
                exam.getEndDate(),
                exam.isPublished(),
                exam.isScheduleLocked(),
                exam.getRejectionNote(),
                subjects,
                List.copyOf(teachers),
                hasSchedule(papers),
                gradeRepository.existsByExamId(exam.getId())
        );
    }

    private static boolean hasSchedule(List<ExamSubject> papers) {
        return !papers.isEmpty() && papers.stream().allMatch(paper -> paper.getExamDate() != null && paper.getStartTime() != null);
    }

    private ExamSubjectResponse toExamSubject(ExamSubject examSubject) {
        Teacher invigilator = examSubject.getInvigilator();
        return new ExamSubjectResponse(
                examSubject.getId(),
                examSubject.getExam().getId(),
                examSubject.getSubject().getId(),
                examSubject.getSubject().getName(),
                examSubject.getMaxMarks(),
                examSubject.getPassMarks(),
                examSubject.getExamDate(),
                examSubject.getStartTime(),
                examSubject.getEndTime(),
                examSubject.getVenue(),
                invigilator != null ? invigilator.getId() : null,
                invigilator != null ? invigilator.getUser().getName() : null
        );
    }
}
