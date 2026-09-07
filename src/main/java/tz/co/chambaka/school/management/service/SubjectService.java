package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SubjectRequest;
import tz.co.chambaka.school.management.dto.academic.SubjectResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import tz.co.chambaka.school.management.repository.SubjectRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final AcademicMapper academicMapper;
    private final ExamSubjectRepository examSubjectRepository;
    private final GradeRepository gradeRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TimetableSlotRepository timetableSlotRepository;

    public SubjectService(
            SubjectRepository subjectRepository,
            AcademicMapper academicMapper,
            ExamSubjectRepository examSubjectRepository,
            GradeRepository gradeRepository,
            TeacherSubjectRepository teacherSubjectRepository,
            TimetableSlotRepository timetableSlotRepository
    ) {
        this.subjectRepository = subjectRepository;
        this.academicMapper = academicMapper;
        this.examSubjectRepository = examSubjectRepository;
        this.gradeRepository = gradeRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.timetableSlotRepository = timetableSlotRepository;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> list(Long schoolId) {
        return subjectRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().map(academicMapper::toSubject).toList();
    }

    @Transactional
    public SubjectResponse create(Long schoolId, SubjectRequest request) {
        if (subjectRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, request.code())) {
            throw new DuplicateResourceException("Subject code already exists");
        }
        Subject subject = new Subject();
        subject.setSchoolId(schoolId);
        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setDescription(request.description());
        return academicMapper.toSubject(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse update(Long schoolId, Long id, SubjectRequest request) {
        Subject subject = require(schoolId, id);
        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setDescription(request.description());
        return academicMapper.toSubject(subject);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        Subject subject = require(schoolId, id);
        if (examSubjectRepository.existsBySubjectId(id)) {
            throw new BusinessException("Remove this subject from exams first");
        }
        if (gradeRepository.existsBySubjectId(id)) {
            throw new BusinessException("Remove grades for this subject first");
        }
        teacherSubjectRepository.deleteBySubjectId(id);
        timetableSlotRepository.deleteBySubjectId(id);
        subjectRepository.delete(subject);
    }

    public Subject require(Long schoolId, Long id) {
        return subjectRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", id));
    }
}
