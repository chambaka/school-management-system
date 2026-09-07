package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final StudentParentRepository studentParentRepository;
    private final UserAccountService userAccountService;
    private final AcademicYearService academicYearService;
    private final ClassService classService;
    private final SectionService sectionService;
    private final PhotoStorageService photoStorageService;

    public StudentService(
            StudentRepository studentRepository,
            StudentParentRepository studentParentRepository,
            UserAccountService userAccountService,
            AcademicYearService academicYearService,
            ClassService classService,
            SectionService sectionService,
            PhotoStorageService photoStorageService
    ) {
        this.studentRepository = studentRepository;
        this.studentParentRepository = studentParentRepository;
        this.userAccountService = userAccountService;
        this.academicYearService = academicYearService;
        this.classService = classService;
        this.sectionService = sectionService;
        this.photoStorageService = photoStorageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(Long schoolId, Long classId, boolean archived, Pageable pageable) {
        return PageResponse.of(studentRepository
                .search(schoolId, classId, archived, StudentStatus.ARCHIVED, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public StudentResponse create(Long schoolId, CreateStudentRequest request) {
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), Role.STUDENT, request.phone());
        Student student = new Student();
        student.setSchoolId(schoolId);
        student.setUser(user);
        student.setAdmissionNo(nextAdmissionNo(schoolId));
        student.setRollNumber(request.rollNumber());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setBloodGroup(request.bloodGroup());
        student.setAdmissionDate(request.admissionDate());
        student.setAddress(request.address());
        student.setEmergencyContact(request.emergencyContact());
        student.setStatus(StudentStatus.ACTIVE);
        applyPlacement(schoolId, student, request.academicYearId(), request.schoolClassId(), request.sectionId());
        Student saved = studentRepository.save(student);
        log.info("Created student id={} schoolId={} admissionNo={}", saved.getId(), schoolId, saved.getAdmissionNo());
        return toResponse(saved);
    }

    @Transactional
    public StudentResponse update(Long schoolId, Long id, UpdateStudentRequest request) {
        Student student = require(schoolId, id);
        User user = student.getUser();
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(PhoneNumbers.persist(request.phone()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.rollNumber() != null) {
            student.setRollNumber(request.rollNumber());
        }
        if (request.dateOfBirth() != null) {
            student.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            student.setGender(request.gender());
        }
        if (request.bloodGroup() != null) {
            student.setBloodGroup(request.bloodGroup());
        }
        if (request.address() != null) {
            student.setAddress(request.address());
        }
        if (request.emergencyContact() != null) {
            student.setEmergencyContact(request.emergencyContact());
        }
        applyPlacement(schoolId, student, request.academicYearId(), request.schoolClassId(), request.sectionId());
        return toResponse(student);
    }

    @Transactional
    public StudentResponse suspend(Long schoolId, Long id) {
        return setStatus(schoolId, id, StudentStatus.SUSPENDED);
    }

    @Transactional
    public StudentResponse archive(Long schoolId, Long id) {
        return setStatus(schoolId, id, StudentStatus.ARCHIVED);
    }

    @Transactional
    public StudentResponse restore(Long schoolId, Long id) {
        return setStatus(schoolId, id, StudentStatus.ACTIVE);
    }

    public Student require(Long schoolId, Long id) {
        return studentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", id));
    }

    public Student requireByUser(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user"));
    }

    @Transactional
    public StudentResponse uploadPhoto(Long schoolId, Long id, MultipartFile file) {
        Student student = require(schoolId, id);
        photoStorageService.storeStudentPhoto(schoolId, id, file);
        student.getUser().setAvatarUrl(photoPath(id));
        log.info("Uploaded student photo id={} schoolId={}", id, schoolId);
        return toResponse(student);
    }

    @Transactional
    public StudentResponse deletePhoto(Long schoolId, Long id) {
        Student student = require(schoolId, id);
        photoStorageService.deleteStudentPhoto(schoolId, id);
        student.getUser().setAvatarUrl(null);
        log.info("Deleted student photo id={} schoolId={}", id, schoolId);
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public StoredPhoto photoFile(Long schoolId, Long id) {
        require(schoolId, id);
        return photoStorageService.findStudentPhoto(schoolId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Student photo not found"));
    }

    static String photoPath(Long studentId) {
        return "/api/v1/students/" + studentId + "/photo";
    }

    String nextAdmissionNo(Long schoolId) {
        String prefix = "ADM-" + LocalDate.now().getYear() + "-";
        long next = studentRepository.countBySchoolIdAndAdmissionNoStartingWithIgnoreCase(schoolId, prefix) + 1;
        String admissionNo;
        do {
            admissionNo = prefix + String.format("%04d", next);
            next++;
        } while (studentRepository.existsBySchoolIdAndAdmissionNoIgnoreCase(schoolId, admissionNo));
        return admissionNo;
    }

    private void applyPlacement(Long schoolId, Student student, Long yearId, Long classId, Long sectionId) {
        if (yearId != null) {
            student.setAcademicYear(academicYearService.require(schoolId, yearId));
        }
        if (classId != null) {
            student.setSchoolClass(classService.require(schoolId, classId));
        }
        if (sectionId != null) {
            student.setSection(sectionService.require(schoolId, sectionId));
        }
    }

    private StudentResponse toResponse(Student student) {
        User user = student.getUser();
        AcademicYear year = student.getAcademicYear();
        SchoolClass schoolClass = student.getSchoolClass();
        Section section = student.getSection();
        return new StudentResponse(
                student.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                student.getAdmissionNo(),
                student.getRollNumber(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getBloodGroup(),
                student.getAdmissionDate(),
                student.getAddress(),
                student.getEmergencyContact(),
                year != null ? year.getId() : null,
                schoolClass != null ? schoolClass.getId() : null,
                schoolClass != null ? schoolClass.getName() : null,
                section != null ? section.getId() : null,
                section != null ? section.getName() : null,
                user.isEnabled(),
                statusOf(student),
                user.getAvatarUrl(),
                studentParentRepository.findByStudentId(student.getId()).stream().map(this::toLink).toList()
        );
    }

    private StudentResponse setStatus(Long schoolId, Long id, StudentStatus status) {
        Student student = require(schoolId, id);
        student.setStatus(status);
        student.getUser().setEnabled(status == StudentStatus.ACTIVE);
        log.info("Set student status id={} schoolId={} status={}", id, schoolId, status);
        return toResponse(student);
    }

    private StudentParentResponse toLink(StudentParent link) {
        return new StudentParentResponse(
                link.getId(),
                link.getStudent().getId(),
                link.getStudent().getUser().getName(),
                link.getStudent().getAdmissionNo(),
                link.getParent().getId(),
                link.getParent().getUser().getName(),
                link.getRelationship(),
                link.isPrimaryContact()
        );
    }

    static StudentStatus statusOf(Student student) {
        return student.getStatus() == null ? StudentStatus.ACTIVE : student.getStatus();
    }
}
