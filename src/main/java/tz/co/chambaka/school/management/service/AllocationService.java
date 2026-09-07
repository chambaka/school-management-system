package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AllocationRequest;
import tz.co.chambaka.school.management.dto.academic.AllocationResponse;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AllocationService {

    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final ClassService classService;
    private final SectionService sectionService;
    private final AcademicYearService academicYearService;

    public AllocationService(
            TeacherSubjectRepository teacherSubjectRepository,
            TeacherService teacherService,
            SubjectService subjectService,
            ClassService classService,
            SectionService sectionService,
            AcademicYearService academicYearService
    ) {
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.teacherService = teacherService;
        this.subjectService = subjectService;
        this.classService = classService;
        this.sectionService = sectionService;
        this.academicYearService = academicYearService;
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> list(Long schoolId, Long academicYearId, Long teacherId) {
        List<TeacherSubject> allocations;
        if (teacherId != null) {
            allocations = teacherSubjectRepository.findBySchoolIdAndTeacherId(schoolId, teacherId);
        } else if (academicYearId != null) {
            allocations = teacherSubjectRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId);
        } else {
            allocations = teacherSubjectRepository.findBySchoolId(schoolId);
        }
        return allocations.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AllocationResponse create(Long schoolId, AllocationRequest request) {
        if (teacherSubjectRepository.existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
                request.teacherId(), request.subjectId(), request.schoolClassId(), request.sectionId(),
                request.academicYearId())) {
            throw new DuplicateResourceException("This subject allocation already exists");
        }
        TeacherSubject allocation = new TeacherSubject();
        allocation.setSchoolId(schoolId);
        allocation.setTeacher(teacherService.require(schoolId, request.teacherId()));
        allocation.setSubject(subjectService.require(schoolId, request.subjectId()));
        allocation.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
        allocation.setSection(request.sectionId() == null ? null : sectionService.require(schoolId, request.sectionId()));
        allocation.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        return toResponse(teacherSubjectRepository.save(allocation));
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        TeacherSubject allocation = teacherSubjectRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeacherSubject", id));
        teacherSubjectRepository.delete(allocation);
    }

    private AllocationResponse toResponse(TeacherSubject allocation) {
        Section section = allocation.getSection();
        return new AllocationResponse(
                allocation.getId(),
                allocation.getTeacher().getId(),
                allocation.getTeacher().getUser().getName(),
                allocation.getSubject().getId(),
                allocation.getSubject().getName(),
                allocation.getSchoolClass().getId(),
                allocation.getSchoolClass().getName(),
                section != null ? section.getId() : null,
                section != null ? section.getName() : null,
                allocation.getAcademicYear().getId()
        );
    }
}
