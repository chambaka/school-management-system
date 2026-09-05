package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamResponse;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final AcademicYearService academicYearService;
    private final ClassService classService;
    private final SubjectService subjectService;

    public ExamService(
            ExamRepository examRepository,
            ExamSubjectRepository examSubjectRepository,
            AcademicYearService academicYearService,
            ClassService classService,
            SubjectService subjectService
    ) {
        this.examRepository = examRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.academicYearService = academicYearService;
        this.classService = classService;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> list(Long schoolId, Long academicYearId) {
        List<Exam> exams = academicYearId == null
                ? examRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                : examRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateDesc(schoolId, academicYearId);
        return exams.stream().map(this::toExam).toList();
    }

    @Transactional
    public ExamResponse create(Long schoolId, ExamRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Exam end date must be after start date");
        }
        Exam exam = new Exam();
        exam.setSchoolId(schoolId);
        exam.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        exam.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
        exam.setName(request.name());
        exam.setExamType(request.examType());
        exam.setStartDate(request.startDate());
        exam.setEndDate(request.endDate());
        Exam saved = examRepository.save(exam);
        log.info("Created exam id={} schoolId={} name={}", saved.getId(), schoolId, saved.getName());
        return toExam(saved);
    }

    @Transactional
    public ExamResponse publish(Long schoolId, Long id, boolean published) {
        Exam exam = require(schoolId, id);
        exam.setPublished(published);
        log.info("Set exam published={} examId={} schoolId={}", published, id, schoolId);
        return toExam(exam);
    }

    @Transactional
    public ExamSubjectResponse addSubject(Long schoolId, Long examId, ExamSubjectRequest request) {
        Exam exam = require(schoolId, examId);
        if (examSubjectRepository.existsByExamIdAndSubjectId(examId, request.subjectId())) {
            throw new DuplicateResourceException("Subject is already added to this exam");
        }
        if (request.passMarks().compareTo(request.maxMarks()) > 0) {
            throw new BusinessException("Pass marks cannot exceed max marks");
        }
        ExamSubject examSubject = new ExamSubject();
        examSubject.setSchoolId(schoolId);
        examSubject.setExam(exam);
        examSubject.setSubject(subjectService.require(schoolId, request.subjectId()));
        examSubject.setMaxMarks(request.maxMarks());
        examSubject.setPassMarks(request.passMarks());
        examSubject.setExamDate(request.examDate());
        return toExamSubject(examSubjectRepository.save(examSubject));
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectResponse> listSubjects(Long schoolId, Long examId) {
        require(schoolId, examId);
        return examSubjectRepository.findByExamId(examId).stream().map(this::toExamSubject).toList();
    }

    public Exam require(Long schoolId, Long id) {
        return examRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", id));
    }

    private ExamResponse toExam(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getAcademicYear().getId(),
                exam.getSchoolClass().getId(),
                exam.getSchoolClass().getName(),
                exam.getName(),
                exam.getExamType(),
                exam.getStartDate(),
                exam.getEndDate(),
                exam.isPublished()
        );
    }

    private ExamSubjectResponse toExamSubject(ExamSubject examSubject) {
        return new ExamSubjectResponse(
                examSubject.getId(),
                examSubject.getExam().getId(),
                examSubject.getSubject().getId(),
                examSubject.getSubject().getName(),
                examSubject.getMaxMarks(),
                examSubject.getPassMarks(),
                examSubject.getExamDate()
        );
    }
}
