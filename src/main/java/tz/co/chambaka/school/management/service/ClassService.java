package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SchoolClassRequest;
import tz.co.chambaka.school.management.dto.academic.SchoolClassResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.Notice;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.FeeStructureRepository;
import tz.co.chambaka.school.management.repository.NoticeRepository;
import tz.co.chambaka.school.management.repository.SchoolClassRepository;
import tz.co.chambaka.school.management.repository.SectionRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearService academicYearService;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final ExamRepository examRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final NoticeRepository noticeRepository;

    public ClassService(
            SchoolClassRepository schoolClassRepository,
            AcademicYearService academicYearService,
            StudentRepository studentRepository,
            SectionRepository sectionRepository,
            ExamRepository examRepository,
            FeeStructureRepository feeStructureRepository,
            TeacherSubjectRepository teacherSubjectRepository,
            NoticeRepository noticeRepository
    ) {
        this.schoolClassRepository = schoolClassRepository;
        this.academicYearService = academicYearService;
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
        this.examRepository = examRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.noticeRepository = noticeRepository;
    }

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> list(Long schoolId, Long academicYearId) {
        List<SchoolClass> classes = academicYearId == null
                ? schoolClassRepository.findBySchoolIdOrderByDisplayOrderAsc(schoolId)
                : schoolClassRepository.findBySchoolIdAndAcademicYearIdOrderByDisplayOrderAsc(schoolId, academicYearId);
        return classes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SchoolClassResponse create(Long schoolId, SchoolClassRequest request) {
        AcademicYear year = academicYearService.require(schoolId, request.academicYearId());
        if (schoolClassRepository.existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCase(
                schoolId, year.getId(), request.code())) {
            throw new DuplicateResourceException("Class code already exists in this academic year");
        }
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setAcademicYear(year);
        schoolClass.setName(request.name());
        schoolClass.setCode(request.code());
        schoolClass.setDisplayOrder(request.displayOrder());
        return toResponse(schoolClassRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassResponse update(Long schoolId, Long id, SchoolClassRequest request) {
        SchoolClass schoolClass = require(schoolId, id);
        AcademicYear year = academicYearService.require(schoolId, request.academicYearId());
        schoolClass.setAcademicYear(year);
        schoolClass.setName(request.name());
        schoolClass.setCode(request.code());
        schoolClass.setDisplayOrder(request.displayOrder());
        return toResponse(schoolClass);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        SchoolClass schoolClass = require(schoolId, id);
        if (studentRepository.countBySchoolClassId(id) > 0) {
            throw new BusinessException("Move or remove students from this class first");
        }
        if (sectionRepository.countBySchoolClassId(id) > 0) {
            throw new BusinessException("Delete sections in this class first");
        }
        if (examRepository.existsBySchoolClassId(id)) {
            throw new BusinessException("Remove exams for this class first");
        }
        if (feeStructureRepository.existsBySchoolClassId(id)) {
            throw new BusinessException("Remove fee structures for this class first");
        }
        teacherSubjectRepository.deleteBySchoolClassId(id);
        for (Notice notice : noticeRepository.findBySchoolClassId(id)) {
            notice.setSchoolClass(null);
        }
        schoolClassRepository.delete(schoolClass);
    }

    public SchoolClass require(Long schoolId, Long id) {
        return schoolClassRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("SchoolClass", id));
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass) {
        return new SchoolClassResponse(
                schoolClass.getId(),
                schoolClass.getAcademicYear().getId(),
                schoolClass.getAcademicYear().getName(),
                schoolClass.getName(),
                schoolClass.getCode(),
                schoolClass.getDisplayOrder()
        );
    }
}
