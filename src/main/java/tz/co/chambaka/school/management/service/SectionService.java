package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SectionRequest;
import tz.co.chambaka.school.management.dto.academic.SectionResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.repository.SectionRepository;
import tz.co.chambaka.school.management.repository.StudentAttendanceRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ClassService classService;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;

    public SectionService(
            SectionRepository sectionRepository,
            ClassService classService,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            TeacherSubjectRepository teacherSubjectRepository,
            TimetableSlotRepository timetableSlotRepository,
            StudentAttendanceRepository studentAttendanceRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.classService = classService;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.studentAttendanceRepository = studentAttendanceRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> list(Long schoolId, Long schoolClassId) {
        return sectionRepository.findBySchoolIdAndSchoolClassIdOrderByNameAsc(schoolId, schoolClassId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SectionResponse create(Long schoolId, SectionRequest request) {
        SchoolClass schoolClass = classService.require(schoolId, request.schoolClassId());
        if (sectionRepository.existsBySchoolClassIdAndNameIgnoreCase(schoolClass.getId(), request.name())) {
            throw new DuplicateResourceException("Section already exists in this class");
        }
        Section section = new Section();
        section.setSchoolId(schoolId);
        section.setSchoolClass(schoolClass);
        section.setName(request.name());
        section.setCapacity(request.capacity());
        section.setClassTeacher(resolveTeacher(schoolId, request.classTeacherId()));
        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public SectionResponse update(Long schoolId, Long id, SectionRequest request) {
        Section section = require(schoolId, id);
        section.setName(request.name());
        section.setCapacity(request.capacity());
        section.setClassTeacher(resolveTeacher(schoolId, request.classTeacherId()));
        return toResponse(section);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        Section section = require(schoolId, id);
        if (studentRepository.countBySectionId(id) > 0) {
            throw new BusinessException("Move or remove students from this section first");
        }
        teacherSubjectRepository.deleteBySectionId(id);
        timetableSlotRepository.deleteBySectionId(id);
        studentAttendanceRepository.deleteBySectionId(id);
        sectionRepository.delete(section);
    }

    public Section require(Long schoolId, Long id) {
        return sectionRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Section", id));
    }

    private Teacher resolveTeacher(Long schoolId, Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        return teacherRepository.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", teacherId));
    }

    private SectionResponse toResponse(Section section) {
        Teacher teacher = section.getClassTeacher();
        return new SectionResponse(
                section.getId(),
                section.getSchoolClass().getId(),
                section.getSchoolClass().getName(),
                section.getName(),
                section.getCapacity(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getUser().getName() : null
        );
    }
}
