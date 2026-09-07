package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.dto.school.UpdateSchoolRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.model.enums.TenantStatus;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SchoolService {

    private static final Logger log = LoggerFactory.getLogger(SchoolService.class);

    private final SchoolRepository schoolRepository;
    private final TenantRepository tenantRepository;
    private final SchoolMapper schoolMapper;
    private final UserAccountService userAccountService;

    public SchoolService(
            SchoolRepository schoolRepository,
            TenantRepository tenantRepository,
            SchoolMapper schoolMapper,
            UserAccountService userAccountService
    ) {
        this.schoolRepository = schoolRepository;
        this.tenantRepository = tenantRepository;
        this.schoolMapper = schoolMapper;
        this.userAccountService = userAccountService;
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> list() {
        List<School> schools = schoolRepository.findAll().stream()
                .filter(school -> school.getStatus() != SchoolStatus.ARCHIVED)
                .filter(school -> school.getTenantId() == null || tenantRepository.findById(school.getTenantId())
                        .map(tenant -> tenant.getStatus() != TenantStatus.ARCHIVED)
                        .orElse(true))
                .toList();
        Map<Long, String> tenantNames = tenantRepository.findAllById(schools.stream()
                        .map(School::getTenantId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (left, right) -> left));
        return schools.stream()
                .map(school -> schoolMapper.toResponse(school).withTenantName(tenantNames.get(school.getTenantId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public BrandingResponse brandingBySlug(String slug) {
        School school = schoolRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("School not found for slug: " + slug));
        if (school.getStatus() == SchoolStatus.ARCHIVED) {
            throw new ResourceNotFoundException("School not found for slug: " + slug);
        }
        return schoolMapper.toBranding(school);
    }

    @Transactional(readOnly = true)
    public BrandingResponse brandingByHost(String host) {
        return schoolRepository.findByCustomDomain(host)
                .filter(school -> school.getStatus() != SchoolStatus.ARCHIVED)
                .map(schoolMapper::toBranding)
                .orElseThrow(() -> new ResourceNotFoundException("No school is mapped to this domain"));
    }

    @Transactional
    public SchoolResponse update(Long id, UpdateSchoolRequest request) {
        School school = require(id);
        if (request.name() != null) {
            school.setName(request.name());
        }
        if (request.email() != null) {
            school.setEmail(request.email());
        }
        if (request.phone() != null) {
            school.setPhone(PhoneNumbers.persist(request.phone()));
        }
        if (request.address() != null) {
            school.setAddress(request.address());
        }
        if (request.website() != null) {
            school.setWebsite(request.website());
        }
        if (request.logoUrl() != null) {
            school.setLogoUrl(request.logoUrl());
        }
        if (request.faviconUrl() != null) {
            school.setFaviconUrl(request.faviconUrl());
        }
        if (request.primaryColor() != null) {
            school.setPrimaryColor(request.primaryColor());
        }
        if (request.secondaryColor() != null) {
            school.setSecondaryColor(request.secondaryColor());
        }
        if (request.accentColor() != null) {
            school.setAccentColor(request.accentColor());
        }
        if (request.customDomain() != null) {
            school.setCustomDomain(request.customDomain().isBlank() ? null : request.customDomain());
        }
        if (request.timezone() != null) {
            school.setTimezone(request.timezone());
        }
        if (request.locale() != null) {
            school.setLocale(request.locale());
        }
        if (request.currency() != null) {
            school.setCurrency(request.currency());
        }
        if (request.country() != null) {
            school.setCountry(request.country());
        }
        if (request.status() != null) {
            if (request.status() == SchoolStatus.ARCHIVED) {
                throw new BusinessException("Use organization delete to archive a school");
            }
            school.setStatus(request.status() == SchoolStatus.TRIAL ? SchoolStatus.ACTIVE : request.status());
        }
        if (request.subscriptionPlan() != null) {
            school.setSubscriptionPlan(request.subscriptionPlan());
        }
        if (request.financeEnabled() != null) {
            school.setFinanceEnabled(request.financeEnabled());
        }
        if (request.attendanceEnabled() != null) {
            school.setAttendanceEnabled(request.attendanceEnabled());
        }
        if (request.examsEnabled() != null) {
            school.setExamsEnabled(request.examsEnabled());
        }
        log.info("Updated school id={} name={} status={}", school.getId(), school.getName(), school.getStatus());
        return toResponse(school);
    }

    @Transactional
    public void archive(Long id) {
        School school = require(id);
        school.setStatus(SchoolStatus.ARCHIVED);
        school.setTrialEndsAt(null);
        userAccountService.deleteForSchool(id);
        log.info("Archived school id={} and removed its users from live", school.getId());
    }

    private SchoolResponse toResponse(School school) {
        String tenantName = school.getTenantId() == null
                ? null
                : tenantRepository.findById(school.getTenantId()).map(Tenant::getName).orElse(null);
        return schoolMapper.toResponse(school).withTenantName(tenantName);
    }

    public School require(Long id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("School", id));
        if (school.getStatus() == SchoolStatus.ARCHIVED) {
            throw ResourceNotFoundException.of("School", id);
        }
        return school;
    }
}
