package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.Grade;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeService.class);

    private final GradeRepository gradeRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamService examService;
    private final StudentService studentService;
    private final TeacherService teacherService;

    public GradeService(
            GradeRepository gradeRepository,
            ExamSubjectRepository examSubjectRepository,
            ExamService examService,
            StudentService studentService,
            TeacherService teacherService
    ) {
        this.gradeRepository = gradeRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.examService = examService;
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    @Transactional
    public GradeResponse record(Long schoolId, GradeRequest request, Long currentUserId) {
        Exam exam = examService.require(schoolId, request.examId());
        Student student = studentService.require(schoolId, request.studentId());
        ExamSubject examSubject = examSubjectRepository.findByExamIdAndSubjectId(request.examId(), request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject is not part of this exam"));
        if (request.marksObtained().compareTo(BigDecimal.ZERO) < 0
                || request.marksObtained().compareTo(examSubject.getMaxMarks()) > 0) {
            throw new BusinessException("Marks must be between 0 and " + examSubject.getMaxMarks());
        }
        if (gradeRepository.existsByExamIdAndStudentIdAndSubjectId(exam.getId(), student.getId(), request.subjectId())) {
            throw new DuplicateResourceException("Grade already recorded for this student and subject");
        }
        Teacher grader = teacherService.requireByUserSafe(currentUserId);
        Grade grade = new Grade();
        grade.setSchoolId(schoolId);
        grade.setExam(exam);
        grade.setExamSubject(examSubject);
        grade.setStudent(student);
        grade.setSubject(examSubject.getSubject());
        grade.setMarksObtained(request.marksObtained());
        grade.setRemarks(request.remarks());
        grade.setGradedBy(grader);
        Grade saved = gradeRepository.save(grade);
        log.info("Recorded grade id={} examId={} studentId={} subjectId={}",
                saved.getId(), exam.getId(), student.getId(), request.subjectId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GradeResponse> byExam(Long schoolId, Long examId) {
        examService.require(schoolId, examId);
        return gradeRepository.findByExamIdAndSchoolId(examId, schoolId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportCardResponse reportCard(Long schoolId, Long studentId, Long examId) {
        Student student = studentService.require(schoolId, studentId);
        Exam exam = examService.require(schoolId, examId);
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
                letterGrade(percentage)
        );
    }

    private String letterGrade(BigDecimal percentage) {
        int value = percentage.intValue();
        if (value >= 80) {
            return "A";
        }
        if (value >= 70) {
            return "B";
        }
        if (value >= 60) {
            return "C";
        }
        if (value >= 50) {
            return "D";
        }
        return "F";
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
