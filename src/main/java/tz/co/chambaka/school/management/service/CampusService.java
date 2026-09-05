package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.campus.CampusResponse;
import tz.co.chambaka.school.management.dto.campus.CreateCampusRequest;
import tz.co.chambaka.school.management.dto.campus.UpdateCampusRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.CampusMapper;
import tz.co.chambaka.school.management.model.Campus;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.repository.CampusRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CampusService {

    private static final Logger log = LoggerFactory.getLogger(CampusService.class);

    private final CampusRepository campusRepository;
    private final SchoolRepository schoolRepository;
    private final CampusMapper campusMapper;

    public CampusService(
            CampusRepository campusRepository,
            SchoolRepository schoolRepository,
            CampusMapper campusMapper
    ) {
        this.campusRepository = campusRepository;
        this.schoolRepository = schoolRepository;
        this.campusMapper = campusMapper;
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> listBySchool(Long schoolId) {
        requireSchool(schoolId);
        return campusRepository.findBySchoolIdOrderByPrimaryCampusDescNameAsc(schoolId).stream()
                .map(campusMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> listByTenant(Long tenantId) {
        return campusRepository.findByTenantIdOrderBySchoolIdAscNameAsc(tenantId).stream()
                .map(campusMapper::toResponse)
                .toList();
    }

    @Transactional
    public CampusResponse create(Long schoolId, CreateCampusRequest request) {
        School school = requireSchool(schoolId);
        String code = normalizeCode(request.code(), request.name());
        if (campusRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, code)) {
            throw new DuplicateResourceException("Campus code is already used at this school");
        }
        boolean makePrimary = Boolean.TRUE.equals(request.primaryCampus());
        if (makePrimary) {
            clearPrimary(schoolId);
        }
        Campus campus = new Campus();
        campus.setTenantId(school.getTenantId());
        campus.setSchoolId(schoolId);
        campus.setName(request.name());
        campus.setCode(code);
        campus.setAddress(request.address());
        campus.setPhone(request.phone());
        campus.setEmail(request.email());
        campus.setTimezone(request.timezone() != null ? request.timezone() : school.getTimezone());
        campus.setPrimaryCampus(makePrimary || campusRepository.countBySchoolId(schoolId) == 0);
        campus = campusRepository.save(campus);
        log.info("Created campus id={} schoolId={} code={}", campus.getId(), schoolId, campus.getCode());
        return campusMapper.toResponse(campus);
    }

    @Transactional
    public CampusResponse update(Long schoolId, Long campusId, UpdateCampusRequest request) {
        Campus campus = requireInSchool(campusId, schoolId);
        if (request.name() != null) {
            campus.setName(request.name());
        }
        if (request.code() != null && !request.code().isBlank()) {
            String code = normalizeCode(request.code(), campus.getName());
            if (!code.equalsIgnoreCase(campus.getCode())
                    && campusRepository.existsBySchoolIdAndCodeIgnoreCase(schoolId, code)) {
                throw new DuplicateResourceException("Campus code is already used at this school");
            }
            campus.setCode(code);
        }
        if (request.address() != null) {
            campus.setAddress(request.address());
        }
        if (request.phone() != null) {
            campus.setPhone(request.phone());
        }
        if (request.email() != null) {
            campus.setEmail(request.email());
        }
        if (request.timezone() != null) {
            campus.setTimezone(request.timezone());
        }
        if (Boolean.TRUE.equals(request.primaryCampus()) && !campus.isPrimaryCampus()) {
            clearPrimary(schoolId);
            campus.setPrimaryCampus(true);
        }
        log.info("Updated campus id={} schoolId={}", campus.getId(), schoolId);
        return campusMapper.toResponse(campus);
    }

    @Transactional
    public void delete(Long schoolId, Long campusId) {
        Campus campus = requireInSchool(campusId, schoolId);
        if (campus.isPrimaryCampus()) {
            throw new BusinessException("Cannot delete the primary campus");
        }
        if (campusRepository.countBySchoolId(schoolId) <= 1) {
            throw new BusinessException("A school must keep at least one campus");
        }
        campusRepository.delete(campus);
        log.info("Deleted campus id={} schoolId={}", campusId, schoolId);
    }

    public Campus requirePrimary(Long schoolId) {
        return campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Primary campus not found"));
    }

    public Campus requireInSchool(Long campusId, Long schoolId) {
        return campusRepository.findByIdAndSchoolId(campusId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Campus", campusId));
    }

    private void clearPrimary(Long schoolId) {
        campusRepository.findBySchoolIdOrderByPrimaryCampusDescNameAsc(schoolId)
                .forEach(existing -> existing.setPrimaryCampus(false));
    }

    private School requireSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("School", schoolId));
    }

    private static String normalizeCode(String code, String name) {
        String source = code != null && !code.isBlank() ? code : name;
        String slug = SlugUtil.slugify(source).replace("-", "").toUpperCase(Locale.ROOT);
        if (slug.isBlank()) {
            return "CAMPUS";
        }
        return slug.length() > 30 ? slug.substring(0, 30) : slug;
    }
}
