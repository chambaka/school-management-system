package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.LessonLogRequest;
import tz.co.chambaka.school.management.dto.academic.LessonLogResponse;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.LessonLog;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.repository.LessonLogRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LessonLogService {

    private final LessonLogRepository lessonLogRepository;
    private final TeacherService teacherService;
    private final SectionService sectionService;
    private final SubjectService subjectService;
    private final TimetableSlotRepository timetableSlotRepository;

    public LessonLogService(
            LessonLogRepository lessonLogRepository,
            TeacherService teacherService,
            SectionService sectionService,
            SubjectService subjectService,
            TimetableSlotRepository timetableSlotRepository
    ) {
        this.lessonLogRepository = lessonLogRepository;
        this.teacherService = teacherService;
        this.sectionService = sectionService;
        this.subjectService = subjectService;
        this.timetableSlotRepository = timetableSlotRepository;
    }

    @Transactional
    public LessonLogResponse create(Long schoolId, Long userId, LessonLogRequest request) {
        Teacher teacher = teacherService.requireByUser(userId);
        LessonLog log = new LessonLog();
        log.setSchoolId(schoolId);
        log.setTeacher(teacher);
        log.setSection(sectionService.require(schoolId, request.sectionId()));
        log.setSubject(subjectService.require(schoolId, request.subjectId()));
        log.setLessonDate(request.lessonDate());
        log.setTopic(request.topic());
        log.setObjectives(request.objectives());
        log.setMaterials(request.materials());
        log.setHomework(request.homework());
        if (request.timetableSlotId() != null) {
            log.setTimetableSlot(timetableSlotRepository.findByIdAndSchoolId(request.timetableSlotId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("TimetableSlot", request.timetableSlotId())));
        }
        return toResponse(lessonLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<LessonLogResponse> bySection(Long schoolId, Long sectionId, LocalDate date) {
        return lessonLogRepository.findBySchoolIdAndSectionIdAndLessonDate(schoolId, sectionId, date)
                .stream().map(this::toResponse).toList();
    }

    private LessonLogResponse toResponse(LessonLog log) {
        return new LessonLogResponse(
                log.getId(),
                log.getTimetableSlot() != null ? log.getTimetableSlot().getId() : null,
                log.getTeacher().getId(),
                log.getTeacher().getUser().getName(),
                log.getSection().getId(),
                log.getSection().getName(),
                log.getSubject().getId(),
                log.getSubject().getName(),
                log.getLessonDate(),
                log.getTopic(),
                log.getObjectives(),
                log.getMaterials(),
                log.getHomework()
        );
    }
}
