package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ClassroomRequest;
import tz.co.chambaka.school.management.dto.academic.ClassroomResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final AcademicMapper academicMapper;

    public ClassroomService(
            ClassroomRepository classroomRepository,
            TimetableSlotRepository timetableSlotRepository,
            AcademicMapper academicMapper
    ) {
        this.classroomRepository = classroomRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.academicMapper = academicMapper;
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> list(Long schoolId) {
        return classroomRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().map(academicMapper::toClassroom).toList();
    }

    @Transactional
    public ClassroomResponse create(Long schoolId, ClassroomRequest request) {
        String name = request.name().trim();
        String code = blankToNull(request.code());
        if (classroomRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new DuplicateResourceException("Classroom name already exists");
        }
        if (code != null && classroomRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, code)) {
            throw new DuplicateResourceException("Classroom code already exists");
        }
        Classroom classroom = new Classroom();
        classroom.setSchoolId(schoolId);
        apply(classroom, name, code, request);
        return academicMapper.toClassroom(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse update(Long schoolId, Long id, ClassroomRequest request) {
        Classroom classroom = require(schoolId, id);
        String name = request.name().trim();
        String code = blankToNull(request.code());
        if (!classroom.getName().equalsIgnoreCase(name)
                && classroomRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new DuplicateResourceException("Classroom name already exists");
        }
        if (code != null
                && (classroom.getCode() == null || !classroom.getCode().equalsIgnoreCase(code))
                && classroomRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, code)) {
            throw new DuplicateResourceException("Classroom code already exists");
        }
        String previousName = classroom.getName();
        apply(classroom, name, code, request);
        if (!previousName.equals(name)) {
            for (TimetableSlot slot : timetableSlotRepository.findBySchoolIdAndRoomIgnoreCase(schoolId, previousName)) {
                slot.setRoom(name);
            }
        }
        return academicMapper.toClassroom(classroom);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        Classroom classroom = require(schoolId, id);
        if (timetableSlotRepository.existsBySchoolIdAndRoomIgnoreCase(schoolId, classroom.getName())) {
            throw new BusinessException("Remove this room from the timetable first");
        }
        classroomRepository.delete(classroom);
    }

    public Classroom require(Long schoolId, Long id) {
        return classroomRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Classroom", id));
    }

    private static void apply(Classroom classroom, String name, String code, ClassroomRequest request) {
        classroom.setName(name);
        classroom.setCode(code);
        classroom.setCapacity(request.capacity());
        classroom.setBuilding(blankToNull(request.building()));
        classroom.setNotes(blankToNull(request.notes()));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
