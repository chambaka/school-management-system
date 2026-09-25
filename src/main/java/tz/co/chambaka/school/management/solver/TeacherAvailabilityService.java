package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.exception.BusinessException;
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
        return repository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
                .map(TeacherAvailabilityService::toResponse)
                .toList();
    }

    @Transactional
    public TeacherAvailabilityResponse create(Long schoolId, TeacherAvailabilityRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("End time must be after start time");
        }
        TeacherAvailability row = new TeacherAvailability();
        row.setSchoolId(schoolId);
        row.setTeacher(teacherService.require(schoolId, request.teacherId()));
        row.setDayOfWeek(request.dayOfWeek());
        row.setStartTime(request.startTime());
        row.setEndTime(request.endTime());
        return toResponse(repository.save(row));
    }

    private static TeacherAvailabilityResponse toResponse(TeacherAvailability row) {
        return new TeacherAvailabilityResponse(
                row.getId(), row.getTeacher().getId(), row.getDayOfWeek(), row.getStartTime(), row.getEndTime());
    }
}
