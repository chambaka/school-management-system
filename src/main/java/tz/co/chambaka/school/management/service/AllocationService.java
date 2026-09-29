package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AllocationRequest;
import tz.co.chambaka.school.management.dto.academic.AllocationResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

    @Transactional(readOnly = true)
    public List<AllocationResponse> listMine(Long schoolId, Long userId) {
        Teacher teacher = teacherService.requireByUserSafe(userId);
        if (teacher == null) {
            return List.of();
        }
        return teacherSubjectRepository.findBySchoolIdAndTeacherId(schoolId, teacher.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireTeachesForUser(
            Long schoolId,
            Long userId,
            Long academicYearId,
            Long classId,
            Long subjectId,
            Long sectionId,
            String action
    ) {
        if (!teachesForUser(schoolId, userId, academicYearId, classId, subjectId, sectionId)) {
            throw new BusinessException(
                    "Only the allocated teacher of this subject for this class can " + action + ".");
        }
    }

    @Transactional(readOnly = true)
    public boolean teachesForUser(
            Long schoolId,
            Long userId,
            Long academicYearId,
            Long classId,
            Long subjectId,
            Long sectionId
    ) {
        Teacher teacher = teacherService.requireByUserSafe(userId);
        if (teacher == null || classId == null || subjectId == null) {
            return false;
        }
        return teacherSubjectRepository.findBySchoolIdAndTeacherId(schoolId, teacher.getId()).stream()
                .anyMatch(allocation -> matches(allocation, academicYearId, classId, subjectId, sectionId));
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
        apply(schoolId, allocation, request);
        return toResponse(teacherSubjectRepository.save(allocation));
    }

    @Transactional
    public AllocationResponse update(Long schoolId, Long id, AllocationRequest request) {
        TeacherSubject allocation = teacherSubjectRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeacherSubject", id));
        if (changed(allocation, request)
                && teacherSubjectRepository.existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
                request.teacherId(), request.subjectId(), request.schoolClassId(), request.sectionId(),
                request.academicYearId())) {
            throw new DuplicateResourceException("This subject allocation already exists");
        }
        apply(schoolId, allocation, request);
        return toResponse(allocation);
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
                allocation.getAcademicYear().getId(),
                allocation.getWeeklyLessons() <= 0 ? 5 : allocation.getWeeklyLessons()
        );
    }

    private void apply(Long schoolId, TeacherSubject allocation, AllocationRequest request) {
        allocation.setTeacher(teacherService.require(schoolId, request.teacherId()));
        allocation.setSubject(subjectService.require(schoolId, request.subjectId()));
        allocation.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
        allocation.setSection(request.sectionId() == null ? null : sectionService.require(schoolId, request.sectionId()));
        allocation.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        allocation.setWeeklyLessons(request.weeklyLessons() == null || request.weeklyLessons() < 1 ? 5 : request.weeklyLessons());
    }

    static boolean matches(
            TeacherSubject allocation,
            Long academicYearId,
            Long classId,
            Long subjectId,
            Long sectionId
    ) {
        if (allocation.getSubject() == null || !subjectId.equals(allocation.getSubject().getId())) {
            return false;
        }
        if (allocation.getSchoolClass() == null || !classId.equals(allocation.getSchoolClass().getId())) {
            return false;
        }
        if (academicYearId != null
                && allocation.getAcademicYear() != null
                && !academicYearId.equals(allocation.getAcademicYear().getId())) {
            return false;
        }
        if (sectionId == null) {
            return true;
        }
        return allocation.getSection() == null || sectionId.equals(allocation.getSection().getId());
    }

    private static boolean changed(TeacherSubject allocation, AllocationRequest request) {
        Long sectionId = allocation.getSection() == null ? null : allocation.getSection().getId();
        return !allocation.getTeacher().getId().equals(request.teacherId())
                || !allocation.getSubject().getId().equals(request.subjectId())
                || !allocation.getSchoolClass().getId().equals(request.schoolClassId())
                || !Objects.equals(sectionId, request.sectionId())
                || !allocation.getAcademicYear().getId().equals(request.academicYearId());
    }
}
