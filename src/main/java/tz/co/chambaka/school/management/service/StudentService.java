package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final UserAccountService userAccountService;
    private final AcademicYearService academicYearService;
    private final ClassService classService;
    private final SectionService sectionService;

    public StudentService(
            StudentRepository studentRepository,
            UserAccountService userAccountService,
            AcademicYearService academicYearService,
            ClassService classService,
            SectionService sectionService
    ) {
        this.studentRepository = studentRepository;
        this.userAccountService = userAccountService;
        this.academicYearService = academicYearService;
        this.classService = classService;
        this.sectionService = sectionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(Long schoolId, Long classId, Pageable pageable) {
        var page = classId == null
                ? studentRepository.findBySchoolId(schoolId, pageable)
                : studentRepository.findBySchoolIdAndSchoolClassId(schoolId, classId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public StudentResponse create(Long schoolId, CreateStudentRequest request) {
        if (studentRepository.existsBySchoolIdAndAdmissionNoIgnoreCase(schoolId, request.admissionNo())) {
            throw new DuplicateResourceException("Admission number already exists");
        }
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), Role.STUDENT, request.phone());
        Student student = new Student();
        student.setSchoolId(schoolId);
        student.setUser(user);
        student.setAdmissionNo(request.admissionNo());
        student.setRollNumber(request.rollNumber());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setBloodGroup(request.bloodGroup());
        student.setAdmissionDate(request.admissionDate());
        student.setAddress(request.address());
        student.setEmergencyContact(request.emergencyContact());
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
            user.setPhone(request.phone());
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

    public Student require(Long schoolId, Long id) {
        return studentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", id));
    }

    public Student requireByUser(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user"));
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
                user.isEnabled()
        );
    }
}
