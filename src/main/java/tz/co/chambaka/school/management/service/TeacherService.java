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
import tz.co.chambaka.school.management.model.enums.TeacherStatus;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    private final TeacherRepository teacherRepository;
    private final UserAccountService userAccountService;
    private final DepartmentService departmentService;
    private final PhotoStorageService photoStorageService;
    private final QualificationService qualificationService;

    public TeacherService(
            TeacherRepository teacherRepository,
            UserAccountService userAccountService,
            DepartmentService departmentService,
            PhotoStorageService photoStorageService,
            QualificationService qualificationService
    ) {
        this.teacherRepository = teacherRepository;
        this.userAccountService = userAccountService;
        this.departmentService = departmentService;
        this.photoStorageService = photoStorageService;
        this.qualificationService = qualificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> list(Long schoolId, boolean archived, Pageable pageable) {
        return PageResponse.of(teacherRepository
                .search(schoolId, archived, TeacherStatus.ARCHIVED, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public TeacherResponse create(Long schoolId, CreateTeacherRequest request) {
        String employeeId = blankToNull(request.employeeId());
        if (employeeId != null && teacherRepository.existsBySchoolIdAndEmployeeIdIgnoreCase(schoolId, employeeId)) {
            throw new DuplicateResourceException("Employee ID already exists");
        }
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), Role.TEACHER, request.phone());
        Teacher teacher = new Teacher();
        teacher.setSchoolId(schoolId);
        teacher.setUser(user);
        teacher.setEmployeeId(employeeId);
        teacher.setQualification(resolveQualification(request.qualification(), null));
        teacher.setSpecialization(request.specialization());
        teacher.setDepartment(resolveDepartment(schoolId, request.department()));
        teacher.setJoiningDate(request.joiningDate());
        teacher.setStatus(TeacherStatus.ACTIVE);
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
            teacher.setQualification(resolveQualification(request.qualification(), teacher.getQualification()));
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
        if (request.employeeId() != null) {
            String employeeId = blankToNull(request.employeeId());
            if (employeeId != null
                    && (teacher.getEmployeeId() == null || !teacher.getEmployeeId().equalsIgnoreCase(employeeId))
                    && teacherRepository.existsBySchoolIdAndEmployeeIdIgnoreCase(schoolId, employeeId)) {
                throw new DuplicateResourceException("Employee ID already exists");
            }
            teacher.setEmployeeId(employeeId);
        }
        return toResponse(teacher);
    }

    @Transactional
    public TeacherResponse archive(Long schoolId, Long id) {
        return setStatus(schoolId, id, TeacherStatus.ARCHIVED);
    }

    @Transactional
    public TeacherResponse restore(Long schoolId, Long id) {
        return setStatus(schoolId, id, TeacherStatus.ACTIVE);
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

    @Transactional
    public TeacherResponse uploadPhoto(Long schoolId, Long id, MultipartFile file) {
        Teacher teacher = require(schoolId, id);
        photoStorageService.storeTeacherPhoto(schoolId, id, file);
        teacher.getUser().setAvatarUrl(photoPath(id));
        log.info("Uploaded teacher photo id={} schoolId={}", id, schoolId);
        return toResponse(teacher);
    }

    @Transactional
    public TeacherResponse deletePhoto(Long schoolId, Long id) {
        Teacher teacher = require(schoolId, id);
        photoStorageService.deleteTeacherPhoto(schoolId, id);
        teacher.getUser().setAvatarUrl(null);
        log.info("Deleted teacher photo id={} schoolId={}", id, schoolId);
        return toResponse(teacher);
    }

    @Transactional(readOnly = true)
    public StoredPhoto photoFile(Long schoolId, Long id) {
        require(schoolId, id);
        return photoStorageService.findTeacherPhoto(schoolId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher photo not found"));
    }

    static String photoPath(Long teacherId) {
        return "/api/v1/teachers/" + teacherId + "/photo";
    }

    private String resolveDepartment(Long schoolId, String department) {
        if (department == null || department.isBlank()) {
            return null;
        }
        return departmentService.requireByName(schoolId, department).getName();
    }

    private String resolveQualification(String qualification, String current) {
        if (qualification == null || qualification.isBlank()) {
            return null;
        }
        String trimmed = qualification.trim();
        if (current != null && current.equalsIgnoreCase(trimmed)) {
            return current;
        }
        return qualificationService.requireByName(trimmed);
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
                user.isEnabled(),
                statusOf(teacher),
                user.getAvatarUrl(),
                user.activeLockedUntil(),
                user.isTotpEnabled()
        );
    }

    private TeacherResponse setStatus(Long schoolId, Long id, TeacherStatus status) {
        Teacher teacher = require(schoolId, id);
        teacher.setStatus(status);
        teacher.getUser().setEnabled(status == TeacherStatus.ACTIVE);
        log.info("Set teacher status id={} schoolId={} status={}", id, schoolId, status);
        return toResponse(teacher);
    }

    static TeacherStatus statusOf(Teacher teacher) {
        return teacher.getStatus() == null ? TeacherStatus.ACTIVE : teacher.getStatus();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
