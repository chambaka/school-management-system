package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.TimetableRequest;
import tz.co.chambaka.school.management.dto.academic.TimetableResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.SchoolClass;
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
        assertTimes(request);
        assertNoClashes(schoolId, request, null);
        TimetableSlot slot = new TimetableSlot();
        slot.setSchoolId(schoolId);
        apply(schoolId, slot, request);
        return toResponse(timetableSlotRepository.save(slot));
    }

    @Transactional
    public TimetableResponse update(Long schoolId, Long id, TimetableRequest request) {
        assertTimes(request);
        TimetableSlot slot = require(schoolId, id);
        assertNoClashes(schoolId, request, id);
        apply(schoolId, slot, request);
        return toResponse(slot);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        timetableSlotRepository.delete(require(schoolId, id));
    }

    private TimetableSlot require(Long schoolId, Long id) {
        return timetableSlotRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("TimetableSlot", id));
    }

    private void apply(Long schoolId, TimetableSlot slot, TimetableRequest request) {
        slot.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        slot.setSection(sectionService.require(schoolId, request.sectionId()));
        slot.setSubject(subjectService.require(schoolId, request.subjectId()));
        slot.setTeacher(teacherService.require(schoolId, request.teacherId()));
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setRoom(blankToNull(request.room()));
    }

    private void assertTimes(TimetableRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private void assertNoClashes(Long schoolId, TimetableRequest request, Long excludeId) {
        if (clashes(timetableSlotRepository.findBySectionIdAndDayOfWeek(request.sectionId(), request.dayOfWeek()),
                request, excludeId)) {
            throw new BusinessException("This slot overlaps an existing timetable entry");
        }
        if (clashes(timetableSlotRepository.findByTeacherIdAndDayOfWeek(request.teacherId(), request.dayOfWeek()),
                request, excludeId)) {
            throw new BusinessException("This teacher is already teaching at that time");
        }
        String room = blankToNull(request.room());
        if (room != null && clashes(
                timetableSlotRepository.findBySchoolIdAndRoomIgnoreCaseAndDayOfWeek(schoolId, room, request.dayOfWeek()),
                request, excludeId)) {
            throw new BusinessException("This room is already booked at that time");
        }
    }

    private boolean clashes(List<TimetableSlot> existing, TimetableRequest request, Long excludeId) {
        return existing.stream()
                .filter(slot -> excludeId == null || !excludeId.equals(slot.getId()))
                .anyMatch(slot -> overlaps(slot, request));
    }

    private boolean overlaps(TimetableSlot existing, TimetableRequest request) {
        return existing.getStartTime().isBefore(request.endTime())
                && request.startTime().isBefore(existing.getEndTime());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TimetableResponse toResponse(TimetableSlot slot) {
        AcademicYear year = slot.getAcademicYear();
        SchoolClass schoolClass = slot.getSection().getSchoolClass();
        return new TimetableResponse(
                slot.getId(),
                year != null ? year.getId() : null,
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
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
