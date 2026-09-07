package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.DepartmentRequest;
import tz.co.chambaka.school.management.dto.academic.DepartmentResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.Department;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.repository.DepartmentRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.util.List;

@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    static final List<DefaultDepartment> DEFAULTS = List.of(
            new DefaultDepartment("Languages Department", "English, Kiswahili, and other languages"),
            new DefaultDepartment("Mathematics Department", null),
            new DefaultDepartment("Science Department", null),
            new DefaultDepartment("Social Studies Department", null),
            new DefaultDepartment("Religious Education Department", null),
            new DefaultDepartment("Creative Arts Department", "Art, Music, Drama"),
            new DefaultDepartment("Physical Education & Sports Department", null),
            new DefaultDepartment("ICT / Computer Studies Department", null),
            new DefaultDepartment("Life Skills / Personal Development Department", null),
            new DefaultDepartment("Environmental Studies Department", null),
            new DefaultDepartment("Special Needs / Inclusive Education Department", null),
            new DefaultDepartment("Early Childhood Education Department", null),
            new DefaultDepartment("Assessment & Examinations Department", null)
    );

    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicMapper academicMapper;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            SchoolRepository schoolRepository,
            AcademicMapper academicMapper
    ) {
        this.departmentRepository = departmentRepository;
        this.schoolRepository = schoolRepository;
        this.academicMapper = academicMapper;
    }

    @PostConstruct
    @Transactional
    public void seedDefaultsForExistingSchools() {
        for (School school : schoolRepository.findAll()) {
            if (school.getStatus() != SchoolStatus.ARCHIVED) {
                ensureDefaults(school.getId());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(Long schoolId) {
        return departmentRepository.findBySchoolIdOrderByNameAsc(schoolId)
                .stream().map(academicMapper::toDepartment).toList();
    }

    @Transactional
    public DepartmentResponse create(Long schoolId, DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new DuplicateResourceException("Department name already exists");
        }
        Department department = new Department();
        department.setSchoolId(schoolId);
        department.setName(name);
        department.setDescription(request.description());
        return academicMapper.toDepartment(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long schoolId, Long id, DepartmentRequest request) {
        Department department = require(schoolId, id);
        String name = request.name().trim();
        if (!department.getName().equalsIgnoreCase(name)
                && departmentRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new DuplicateResourceException("Department name already exists");
        }
        department.setName(name);
        department.setDescription(request.description());
        return academicMapper.toDepartment(department);
    }

    public Department require(Long schoolId, Long id) {
        return departmentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    public Department requireByName(Long schoolId, String name) {
        return departmentRepository.findBySchoolIdAndNameIgnoreCase(schoolId, name.trim())
                .orElseThrow(() -> new BusinessException("Department is not configured for this school"));
    }

    @Transactional
    public void ensureDefaults(Long schoolId) {
        int created = 0;
        for (DefaultDepartment seed : DEFAULTS) {
            if (departmentRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, seed.name())) {
                continue;
            }
            Department department = new Department();
            department.setSchoolId(schoolId);
            department.setName(seed.name());
            department.setDescription(seed.description());
            departmentRepository.save(department);
            created++;
        }
        if (created > 0) {
            log.info("Seeded {} default departments for school {}", created, schoolId);
        }
    }

    record DefaultDepartment(String name, String description) {
    }
}
