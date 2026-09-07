package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.teacher.CreateTeacherRequest;
import tz.co.chambaka.school.management.dto.teacher.TeacherResponse;
import tz.co.chambaka.school.management.dto.teacher.UpdateTeacherRequest;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    private final TeacherRepository teacherRepository;
    private final UserAccountService userAccountService;
    private final DepartmentService departmentService;

    public TeacherService(
            TeacherRepository teacherRepository,
            UserAccountService userAccountService,
            DepartmentService departmentService
    ) {
        this.teacherRepository = teacherRepository;
        this.userAccountService = userAccountService;
        this.departmentService = departmentService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> list(Long schoolId, Pageable pageable) {
        return PageResponse.of(teacherRepository.findBySchoolId(schoolId, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public TeacherResponse create(Long schoolId, CreateTeacherRequest request) {
        if (teacherRepository.existsBySchoolIdAndEmployeeIdIgnoreCase(schoolId, request.employeeId())) {
            throw new DuplicateResourceException("Employee ID already exists");
        }
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), Role.TEACHER, request.phone());
        Teacher teacher = new Teacher();
        teacher.setSchoolId(schoolId);
        teacher.setUser(user);
        teacher.setEmployeeId(request.employeeId());
        teacher.setQualification(request.qualification());
        teacher.setSpecialization(request.specialization());
        teacher.setDepartment(resolveDepartment(schoolId, request.department()));
        teacher.setJoiningDate(request.joiningDate());
        Teacher saved = teacherRepository.save(teacher);
        log.info("Created teacher id={} schoolId={} employeeId={}", saved.getId(), schoolId, saved.getEmployeeId());
        return toResponse(saved);
    }

    @Transactional
    public TeacherResponse update(Long schoolId, Long id, UpdateTeacherRequest request) {
        Teacher teacher = require(schoolId, id);
        User user = teacher.getUser();
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(PhoneNumbers.persist(request.phone()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.qualification() != null) {
            teacher.setQualification(request.qualification());
        }
        if (request.specialization() != null) {
            teacher.setSpecialization(request.specialization());
        }
        if (request.department() != null) {
            teacher.setDepartment(resolveDepartment(schoolId, request.department()));
        }
        if (request.joiningDate() != null) {
            teacher.setJoiningDate(request.joiningDate());
        }
        return toResponse(teacher);
    }

    public Teacher require(Long schoolId, Long id) {
        return teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", id));
    }

    public Teacher requireByUser(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for current user"));
    }

    public Teacher requireByUserSafe(Long userId) {
        return teacherRepository.findByUserId(userId).orElse(null);
    }

    private String resolveDepartment(Long schoolId, String department) {
        if (department == null || department.isBlank()) {
            return null;
        }
        return departmentService.requireByName(schoolId, department).getName();
    }

    private TeacherResponse toResponse(Teacher teacher) {
        User user = teacher.getUser();
        return new TeacherResponse(
                teacher.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                teacher.getEmployeeId(),
                teacher.getQualification(),
                teacher.getSpecialization(),
                teacher.getDepartment(),
                teacher.getJoiningDate(),
                user.isEnabled()
        );
    }
}
