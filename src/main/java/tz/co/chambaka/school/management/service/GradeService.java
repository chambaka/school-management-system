package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.academic.ResultMath;
import tz.co.chambaka.school.management.dto.academic.BulkGradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeGridResponse;
import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
import tz.co.chambaka.school.management.dto.academic.MeritRowResponse;
import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.dto.academic.TermReportResponse;
import tz.co.chambaka.school.management.dto.academic.TermResultResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.Grade;
import tz.co.chambaka.school.management.model.ResultWeightConfig;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.enums.AssessmentComponent;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeService.class);

    private final GradeRepository gradeRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamService examService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ResultConfigService resultConfigService;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final AllocationService allocationService;

    public GradeService(
            GradeRepository gradeRepository,
            ExamSubjectRepository examSubjectRepository,
            ExamService examService,
            StudentService studentService,
            TeacherService teacherService,
            ResultConfigService resultConfigService,
            ExamRepository examRepository,
            StudentRepository studentRepository,
            AllocationService allocationService
    ) {
        this.gradeRepository = gradeRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.examService = examService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.resultConfigService = resultConfigService;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.allocationService = allocationService;
    }

    @Transactional
    public GradeResponse record(Long schoolId, GradeRequest request, Long currentUserId) {
        Exam exam = examService.require(schoolId, request.examId());
        examService.assertMarksEditable(exam);
        assertTeachesExamSubject(schoolId, currentUserId, exam, request.subjectId());
        Student student = studentService.require(schoolId, request.studentId());
        ExamSubject examSubject = examSubjectRepository.findByExamIdAndSubjectId(request.examId(), request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject is not part of this exam"));
        if (request.marksObtained().compareTo(BigDecimal.ZERO) < 0
                || request.marksObtained().compareTo(examSubject.getMaxMarks()) > 0) {
            throw new BusinessException("Marks must be between 0 and " + examSubject.getMaxMarks());
        }
        Teacher grader = teacherService.requireByUserSafe(currentUserId);
        Grade grade = gradeRepository
                .findByExamIdAndStudentIdAndSubjectId(exam.getId(), student.getId(), request.subjectId())
                .orElseGet(Grade::new);
        boolean created = grade.getId() == null;
        grade.setSchoolId(schoolId);
        grade.setExam(exam);
        grade.setExamSubject(examSubject);
        grade.setStudent(student);
        grade.setSubject(examSubject.getSubject());
        grade.setMarksObtained(request.marksObtained());
        grade.setRemarks(request.remarks());
        grade.setGradedBy(grader);
        Grade saved = gradeRepository.save(grade);
        log.info("{} grade id={} examId={} studentId={} subjectId={}",
                created ? "Recorded" : "Updated", saved.getId(), exam.getId(), student.getId(), request.subjectId());
        return toResponse(saved);
    }

    @Transactional
    public List<GradeResponse> recordBulk(Long schoolId, BulkGradeRequest request, Long currentUserId) {
        List<GradeResponse> saved = new ArrayList<>();
        for (BulkGradeRequest.Entry entry : request.entries()) {
            saved.add(record(schoolId, new GradeRequest(request.examId(), entry.studentId(), request.subjectId(),
                    entry.marksObtained(), entry.remarks()), currentUserId));
        }
        return saved;
    }

    @Transactional
    public void delete(Long schoolId, Long examId, Long studentId, Long subjectId, Long userId) {
        Exam exam = examService.require(schoolId, examId);
        examService.assertMarksEditable(exam);
        assertTeachesExamSubject(schoolId, userId, exam, subjectId);
        studentService.require(schoolId, studentId);
        Grade grade = gradeRepository
                .findByExamIdAndStudentIdAndSubjectId(examId, studentId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("No marks for this student on this paper"));
        gradeRepository.delete(grade);
        log.info("Deleted grade id={} examId={} studentId={} subjectId={}", grade.getId(), examId, studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<GradeResponse> byExam(Long schoolId, Long examId) {
        examService.require(schoolId, examId);
        return gradeRepository.findByExamIdAndSchoolId(examId, schoolId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GradeGridResponse grid(Long schoolId, Long examId, Long subjectId) {
        Exam exam = examService.require(schoolId, examId);
        ExamSubject paper = examSubjectRepository.findByExamIdAndSubjectId(examId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject is not part of this exam"));
        List<Student> students = studentRepository.findBySchoolIdAndSchoolClassId(schoolId, exam.getSchoolClass().getId())
                .stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == StudentStatus.ACTIVE)
                .sorted(Comparator.comparing(s -> s.getUser().getName()))
                .toList();
        Map<Long, Grade> byStudent = new HashMap<>();
        for (Grade grade : gradeRepository.findByExamIdAndSubjectId(examId, subjectId)) {
            byStudent.put(grade.getStudent().getId(), grade);
        }
        List<GradeGridResponse.Row> ranked = new ArrayList<>();
        for (Student student : students) {
            Grade grade = byStudent.get(student.getId());
            BigDecimal marks = grade == null ? null : grade.getMarksObtained();
            String letter = marks == null ? "" : letterGrade(schoolId, toPercent(marks, paper.getMaxMarks()));
            boolean passed = marks != null && marks.compareTo(paper.getPassMarks()) >= 0;
            ranked.add(new GradeGridResponse.Row(student.getId(), student.getUser().getName(), student.getAdmissionNo(),
                    marks, letter, passed, null));
        }
        ranked.sort(Comparator.comparing((GradeGridResponse.Row row) -> row.marksObtained() == null
                ? BigDecimal.ZERO : row.marksObtained()).reversed());
        List<GradeGridResponse.Row> withPos = new ArrayList<>();
        int pos = 1;
        for (GradeGridResponse.Row row : ranked) {
            withPos.add(new GradeGridResponse.Row(row.studentId(), row.studentName(), row.admissionNo(),
                    row.marksObtained(), row.letterGrade(), row.passed(), row.marksObtained() == null ? null : pos++));
        }
        BigDecimal avg = withPos.stream()
                .map(GradeGridResponse.Row::marksObtained)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long counted = withPos.stream().filter(r -> r.marksObtained() != null).count();
        BigDecimal subjectAverage = counted == 0 ? BigDecimal.ZERO
                : avg.divide(BigDecimal.valueOf(counted), 2, RoundingMode.HALF_UP);
        return new GradeGridResponse(exam.getId(), exam.getName(), paper.getSubject().getId(), paper.getSubject().getName(),
                paper.getMaxMarks(), paper.getPassMarks(), subjectAverage, withPos, examService.marksEditable(exam));
    }

    @Transactional(readOnly = true)
    public ReportCardResponse reportCard(Long schoolId, Long studentId, Long examId) {
        return reportCard(schoolId, studentId, examId, Role.HEADMASTER);
    }

    @Transactional(readOnly = true)
    public ReportCardResponse reportCard(Long schoolId, Long studentId, Long examId, Role role) {
        Student student = studentService.require(schoolId, studentId);
        Exam exam = examService.require(schoolId, examId);
        examService.assertVisibleTo(exam, role);
        List<GradeResponse> subjects = gradeRepository.findByExamIdAndStudentId(examId, studentId)
                .stream().map(this::toResponse).toList();
        BigDecimal totalObtained = subjects.stream()
                .map(GradeResponse::marksObtained)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMax = subjects.stream()
                .map(GradeResponse::maxMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percentage = totalMax.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalObtained.multiply(BigDecimal.valueOf(100)).divide(totalMax, 2, RoundingMode.HALF_UP);
        String overall = letterGrade(schoolId, percentage);
        Integer position = classPosition(schoolId, exam, studentId);
        Long yearId = exam.getAcademicYear() == null ? null : exam.getAcademicYear().getId();
        Long termId = exam.getAcademicTerm() == null ? null : exam.getAcademicTerm().getId();
        List<TermResultResponse> termResults = yearId == null
                ? List.of()
                : termReport(schoolId, studentId, yearId, termId, role).subjects();
        return new ReportCardResponse(
                student.getId(),
                student.getUser().getName(),
                student.getAdmissionNo(),
                student.getSchoolClass() != null ? student.getSchoolClass().getName() : null,
                student.getSection() != null ? student.getSection().getName() : null,
                exam.getId(),
                exam.getName(),
                subjects,
                totalObtained,
                totalMax,
                percentage,
                overall,
                ResultMath.gpaPoints(overall),
                position,
                exam.isPublished(),
                termResults
        );
    }

    @Transactional(readOnly = true)
    public TermResultResponse termResult(Long schoolId, Long studentId, Long subjectId, Long academicYearId, Long termId) {
        return termResult(schoolId, studentId, subjectId, academicYearId, termId, Role.HEADMASTER);
    }

    @Transactional(readOnly = true)
    public TermResultResponse termResult(
            Long schoolId, Long studentId, Long subjectId, Long academicYearId, Long termId, Role role
    ) {
        Student student = studentService.require(schoolId, studentId);
        Long classId = student.getSchoolClass() == null ? null : student.getSchoolClass().getId();
        List<Exam> exams = visibleExams(schoolId, academicYearId, classId, role);
        return computeTermResult(schoolId, student, subjectId, subjectName(exams, studentId, subjectId), academicYearId, termId, exams);
    }

    @Transactional(readOnly = true)
    public TermReportResponse termReport(Long schoolId, Long studentId, Long academicYearId, Long termId, Role role) {
        Student student = studentService.require(schoolId, studentId);
        Long classId = student.getSchoolClass() == null ? null : student.getSchoolClass().getId();
        return buildTermReport(schoolId, student, academicYearId, termId, visibleExams(schoolId, academicYearId, classId, role));
    }

    @Transactional(readOnly = true)
    public List<TermReportResponse> termReports(Long schoolId, Long academicYearId, Long termId, Role role) {
        Map<Long, List<Exam>> examsByClass = new HashMap<>();
        return studentRepository.findBySchoolIdOrderByAdmissionNoAsc(schoolId).stream()
                .filter(student -> student.getStatus() == null || student.getStatus() == StudentStatus.ACTIVE)
                .map(student -> {
                    Long classId = student.getSchoolClass() == null ? null : student.getSchoolClass().getId();
                    long key = classId == null ? 0L : classId;
                    List<Exam> exams = examsByClass.computeIfAbsent(
                            key, ignored -> visibleExams(schoolId, academicYearId, classId, role));
                    return buildTermReport(schoolId, student, academicYearId, termId, exams);
                })
                .sorted(Comparator.comparing(TermReportResponse::className, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(TermReportResponse::admissionNo, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private TermReportResponse buildTermReport(
            Long schoolId, Student student, Long academicYearId, Long termId, List<Exam> exams
    ) {
        List<TermResultResponse> rows = subjectsFor(exams).entrySet().stream()
                .map(entry -> computeTermResult(schoolId, student, entry.getKey(), entry.getValue(), academicYearId, termId, exams))
                .sorted(Comparator.comparing(TermResultResponse::subjectName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        BigDecimal average = averageTerminal(rows);
        Exam sample = exams.isEmpty() ? null : exams.getFirst();
        String yearName = sample != null && sample.getAcademicYear() != null ? sample.getAcademicYear().getName() : null;
        String termName = sample != null && sample.getAcademicTerm() != null ? sample.getAcademicTerm().getName() : null;
        return new TermReportResponse(
                student.getId(),
                student.getUser().getName(),
                student.getAdmissionNo(),
                student.getSchoolClass() != null ? student.getSchoolClass().getName() : null,
                academicYearId,
                yearName,
                termId,
                termName,
                rows,
                average,
                letterGrade(schoolId, average)
        );
    }

    @Transactional(readOnly = true)
    public List<TermResultResponse> classTermResults(
            Long schoolId, Long academicYearId, Long termId, Long classId, Long subjectId, Role role
    ) {
        List<Exam> exams = visibleExams(schoolId, academicYearId, classId, role);
        String subjectName = subjectsFor(exams).getOrDefault(subjectId, "Subject");
        return studentRepository.findBySchoolIdAndSchoolClassId(schoolId, classId).stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == StudentStatus.ACTIVE)
                .map(student -> computeTermResult(schoolId, student, subjectId, subjectName, academicYearId, termId, exams))
                .sorted(Comparator.comparing(TermResultResponse::terminalResult, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TermResultResponse::studentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeritRowResponse> meritList(Long schoolId, Long examId, Role role) {
        Exam exam = examService.require(schoolId, examId);
        examService.assertVisibleTo(exam, role);
        if (exam.getSchoolClass() == null) {
            return List.of();
        }
        List<Student> classmates = studentRepository.findBySchoolIdAndSchoolClassId(schoolId, exam.getSchoolClass().getId());
        record Score(Student student, BigDecimal total, BigDecimal max) {}
        List<Score> scores = classmates.stream().map(student -> {
            var grades = gradeRepository.findByExamIdAndStudentId(exam.getId(), student.getId());
            BigDecimal total = grades.stream().map(Grade::getMarksObtained).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal max = grades.stream().map(g -> g.getExamSubject().getMaxMarks()).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new Score(student, total, max);
        }).sorted((a, b) -> b.total.compareTo(a.total)).toList();
        List<MeritRowResponse> rows = new ArrayList<>();
        int position = 1;
        for (Score score : scores) {
            BigDecimal percent = toPercent(score.total, score.max);
            rows.add(new MeritRowResponse(
                    position++,
                    score.student.getId(),
                    score.student.getUser().getName(),
                    score.student.getAdmissionNo(),
                    score.total,
                    percent,
                    letterGrade(schoolId, percent)
            ));
        }
        return rows;
    }

    private Integer classPosition(Long schoolId, Exam exam, Long studentId) {
        if (exam.getSchoolClass() == null || studentRepository == null) {
            return null;
        }
        List<Student> classmates = studentRepository.findBySchoolIdAndSchoolClassId(schoolId, exam.getSchoolClass().getId());
        List<BigDecimal> totals = new ArrayList<>();
        BigDecimal mine = BigDecimal.ZERO;
        for (Student classmate : classmates) {
            BigDecimal total = gradeRepository.findByExamIdAndStudentId(exam.getId(), classmate.getId()).stream()
                    .map(Grade::getMarksObtained)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totals.add(total);
            if (classmate.getId().equals(studentId)) {
                mine = total;
            }
        }
        totals.sort(Comparator.reverseOrder());
        int pos = 1;
        for (BigDecimal total : totals) {
            if (total.compareTo(mine) == 0) {
                return pos;
            }
            pos++;
        }
        return pos;
    }

    private TermResultResponse computeTermResult(
            Long schoolId,
            Student student,
            Long subjectId,
            String subjectName,
            Long academicYearId,
            Long termId,
            List<Exam> exams
    ) {
        BigDecimal midterm = componentScore(exams, student.getId(), subjectId, AssessmentComponent.MIDTERM);
        BigDecimal semiExam = componentScore(exams, student.getId(), subjectId, AssessmentComponent.SEMI_TERMINAL);
        BigDecimal terminalExam = componentScore(exams, student.getId(), subjectId, AssessmentComponent.TERMINAL);
        ResultWeightConfig resolved = resultConfigService == null
                ? null
                : resultConfigService.resolve(schoolId, academicYearId, termId, subjectId);
        ResultWeightConfig weights = resolved == null ? fallbackWeights() : resolved;
        BigDecimal semi = ResultMath.semiTerminal(midterm, semiExam, weights.getMidtermWeight(), weights.getSemiExamWeight());
        BigDecimal terminal = ResultMath.terminal(semi, terminalExam, weights.getSemiResultWeight(), weights.getTerminalExamWeight());
        return new TermResultResponse(
                student.getId(),
                student.getUser().getName(),
                student.getAdmissionNo(),
                subjectId,
                subjectName,
                midterm,
                semiExam,
                semi,
                terminalExam,
                terminal,
                letterGrade(schoolId, terminal)
        );
    }

    private List<Exam> visibleExams(Long schoolId, Long academicYearId, Long classId, Role role) {
        List<Exam> exams = role == Role.PARENT || role == Role.STUDENT
                ? examRepository.findBySchoolIdAndAcademicYearIdAndPublishedTrueOrderByStartDateDesc(schoolId, academicYearId)
                : examRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateDesc(schoolId, academicYearId);
        if (classId == null) {
            return exams;
        }
        return exams.stream()
                .filter(exam -> exam.getSchoolClass() == null || classId.equals(exam.getSchoolClass().getId()))
                .toList();
    }

    private Map<Long, String> subjectsFor(List<Exam> exams) {
        Map<Long, String> subjects = new LinkedHashMap<>();
        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        if (examIds.isEmpty()) {
            return subjects;
        }
        for (ExamSubject paper : examSubjectRepository.findByExamIdIn(examIds)) {
            Subject subject = paper.getSubject();
            if (subject != null && subject.getId() != null) {
                subjects.putIfAbsent(subject.getId(), subject.getName());
            }
        }
        return subjects;
    }

    private String subjectName(List<Exam> exams, Long studentId, Long subjectId) {
        String fromPapers = subjectsFor(exams).get(subjectId);
        if (fromPapers != null) {
            return fromPapers;
        }
        return exams.stream()
                .flatMap(exam -> gradeRepository.findByExamIdAndStudentId(exam.getId(), studentId).stream())
                .filter(grade -> grade.getSubject() != null && subjectId.equals(grade.getSubject().getId()))
                .map(grade -> grade.getSubject().getName())
                .findFirst()
                .orElse("Subject");
    }

    private static BigDecimal averageTerminal(List<TermResultResponse> rows) {
        if (rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> scores = rows.stream().map(TermResultResponse::terminalResult).filter(score -> score != null).toList();
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal componentScore(List<Exam> exams, Long studentId, Long subjectId, AssessmentComponent component) {
        return exams.stream()
                .filter(exam -> exam.getAssessmentComponent() == component)
                .flatMap(exam -> gradeRepository.findByExamIdAndStudentId(exam.getId(), studentId).stream())
                .filter(grade -> grade.getSubject().getId().equals(subjectId))
                .map(Grade::getMarksObtained)
                .findFirst()
                .orElse(null);
    }

    private String letterGrade(Long schoolId, BigDecimal percentage) {
        if (percentage == null) {
            return "";
        }
        if (resultConfigService != null) {
            return resultConfigService.letterFor(schoolId, percentage);
        }
        return ResultMath.letter(percentage, null);
    }

    private static BigDecimal toPercent(BigDecimal marks, BigDecimal max) {
        if (max == null || max.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return marks.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP);
    }

    private static ResultWeightConfig fallbackWeights() {
        ResultWeightConfig config = new ResultWeightConfig();
        config.setMidtermWeight(ResultMath.DEFAULT_MIDTERM_WEIGHT);
        config.setSemiExamWeight(ResultMath.DEFAULT_SEMI_EXAM_WEIGHT);
        config.setSemiResultWeight(ResultMath.DEFAULT_SEMI_RESULT_WEIGHT);
        config.setTerminalExamWeight(ResultMath.DEFAULT_TERMINAL_EXAM_WEIGHT);
        return config;
    }

    private void assertTeachesExamSubject(Long schoolId, Long userId, Exam exam, Long subjectId) {
        Long yearId = exam.getAcademicYear() == null ? null : exam.getAcademicYear().getId();
        Long classId = exam.getSchoolClass() == null ? null : exam.getSchoolClass().getId();
        allocationService.requireTeachesForUser(
                schoolId, userId, yearId, classId, subjectId, null, "submit marks");
    }

    private GradeResponse toResponse(Grade grade) {
        ExamSubject examSubject = grade.getExamSubject();
        boolean passed = grade.getMarksObtained().compareTo(examSubject.getPassMarks()) >= 0;
        return new GradeResponse(
                grade.getId(),
                grade.getExam().getId(),
                grade.getStudent().getId(),
                grade.getStudent().getUser().getName(),
                grade.getSubject().getId(),
                grade.getSubject().getName(),
                grade.getMarksObtained(),
                examSubject.getMaxMarks(),
                examSubject.getPassMarks(),
                passed,
                grade.getRemarks()
        );
    }
}
