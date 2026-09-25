package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.service.TeacherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherAvailabilityService {

    private final TeacherAvailabilityRepository repository;
    private final TeacherService teacherService;

    public TeacherAvailabilityService(TeacherAvailabilityRepository repository, TeacherService teacherService) {
        this.repository = repository;
        this.teacherService = teacherService;
    }

    @Transactional(readOnly = true)
    public List<TeacherAvailabilityResponse> list(Long schoolId, Long teacherId) {
        List<TeacherAvailability> rows = teacherId == null
                ? repository.findBySchoolIdOrderByDayOfWeekAscStartTimeAsc(schoolId)
                : repository.findBySchoolIdAndTeacherId(schoolId, teacherId);
        return rows.stream().map(TeacherAvailabilityService::toResponse).toList();
    }

    @Transactional
    public TeacherAvailabilityResponse create(Long schoolId, TeacherAvailabilityRequest request) {
        assertTimes(request);
        TeacherAvailability row = new TeacherAvailability();
        row.setSchoolId(schoolId);
        apply(schoolId, row, request);
        return toResponse(repository.save(row));
    }

    @Transactional
    public TeacherAvailabilityResponse update(Long schoolId, Long id, TeacherAvailabilityRequest request) {
        assertTimes(request);
        TeacherAvailability row = require(schoolId, id);
        apply(schoolId, row, request);
        return toResponse(row);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        repository.delete(require(schoolId, id));
    }

    public TeacherAvailability require(Long schoolId, Long id) {
        return repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeacherAvailability", id));
    }

    private void apply(Long schoolId, TeacherAvailability row, TeacherAvailabilityRequest request) {
        row.setTeacher(teacherService.require(schoolId, request.teacherId()));
        row.setDayOfWeek(request.dayOfWeek());
        row.setStartTime(request.startTime());
        row.setEndTime(request.endTime());
    }

    private static void assertTimes(TeacherAvailabilityRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private static TeacherAvailabilityResponse toResponse(TeacherAvailability row) {
        Teacher teacher = row.getTeacher();
        String name = teacher.getUser() == null ? "Teacher" : teacher.getUser().getName();
        return new TeacherAvailabilityResponse(
                row.getId(), teacher.getId(), name, row.getDayOfWeek(), row.getStartTime(), row.getEndTime());
    }
}
