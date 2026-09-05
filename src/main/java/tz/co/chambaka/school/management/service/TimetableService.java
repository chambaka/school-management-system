package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.TimetableRequest;
import tz.co.chambaka.school.management.dto.academic.TimetableResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimetableService {

    private final TimetableSlotRepository timetableSlotRepository;
    private final AcademicYearService academicYearService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final TeacherService teacherService;

    public TimetableService(
            TimetableSlotRepository timetableSlotRepository,
            AcademicYearService academicYearService,
            SectionService sectionService,
            SubjectService subjectService,
            TeacherService teacherService
    ) {
        this.timetableSlotRepository = timetableSlotRepository;
        this.academicYearService = academicYearService;
        this.sectionService = sectionService;
        this.subjectService = subjectService;
        this.teacherService = teacherService;
    }

    @Transactional(readOnly = true)
    public List<TimetableResponse> bySection(Long schoolId, Long sectionId) {
        return timetableSlotRepository.findBySchoolIdAndSectionIdOrderByDayOfWeekAscStartTimeAsc(schoolId, sectionId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableResponse> byTeacher(Long schoolId, Long teacherId) {
        return timetableSlotRepository.findBySchoolIdAndTeacherIdOrderByDayOfWeekAscStartTimeAsc(schoolId, teacherId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TimetableResponse create(Long schoolId, TimetableRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }
        boolean overlap = timetableSlotRepository
                .findBySectionIdAndDayOfWeek(request.sectionId(), request.dayOfWeek())
                .stream()
                .anyMatch(slot -> overlaps(slot, request));
        if (overlap) {
            throw new BusinessException("This slot overlaps an existing timetable entry");
        }
        TimetableSlot slot = new TimetableSlot();
        slot.setSchoolId(schoolId);
        slot.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        slot.setSection(sectionService.require(schoolId, request.sectionId()));
        slot.setSubject(subjectService.require(schoolId, request.subjectId()));
        slot.setTeacher(teacherService.require(schoolId, request.teacherId()));
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setRoom(request.room());
        return toResponse(timetableSlotRepository.save(slot));
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        TimetableSlot slot = timetableSlotRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("TimetableSlot", id));
        timetableSlotRepository.delete(slot);
    }

    private boolean overlaps(TimetableSlot existing, TimetableRequest request) {
        return existing.getStartTime().isBefore(request.endTime())
                && request.startTime().isBefore(existing.getEndTime());
    }

    private TimetableResponse toResponse(TimetableSlot slot) {
        return new TimetableResponse(
                slot.getId(),
                slot.getSection().getId(),
                slot.getSection().getName(),
                slot.getSubject().getId(),
                slot.getSubject().getName(),
                slot.getTeacher().getId(),
                slot.getTeacher().getUser().getName(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getRoom()
        );
    }
}
