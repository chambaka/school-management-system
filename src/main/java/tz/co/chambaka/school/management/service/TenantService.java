package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.school.CreateSchoolRequest;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.dto.tenant.CreateTenantRequest;
import tz.co.chambaka.school.management.dto.tenant.TenantResponse;
import tz.co.chambaka.school.management.dto.tenant.UpdateTenantRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.model.Campus;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.model.enums.TenantStatus;
import tz.co.chambaka.school.management.repository.CampusRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final SchoolRepository schoolRepository;
    private final CampusRepository campusRepository;
    private final SchoolMapper schoolMapper;
    private final SmsProperties smsProperties;

    public TenantService(
            TenantRepository tenantRepository,
            SchoolRepository schoolRepository,
            CampusRepository campusRepository,
            SchoolMapper schoolMapper,
            SmsProperties smsProperties
    ) {
        this.tenantRepository = tenantRepository;
        this.schoolRepository = schoolRepository;
        this.campusRepository = campusRepository;
        this.schoolMapper = schoolMapper;
        this.smsProperties = smsProperties;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        rejectExtraOrganizations();
        return toResponse(createTenant(
                request.name(),
                request.email(),
                request.phone(),
                request.country(),
                request.timezone(),
                request.currency()));
    }

    @Transactional
    public TenantResponse update(Long id, UpdateTenantRequest request) {
        Tenant tenant = require(id);
        if (request.name() != null) {
            tenant.setName(request.name());
        }
        if (request.email() != null) {
            tenant.setEmail(request.email());
        }
        if (request.phone() != null) {
            tenant.setPhone(request.phone());
        }
        if (request.country() != null) {
            tenant.setCountry(request.country());
        }
        if (request.timezone() != null) {
            tenant.setTimezone(request.timezone());
        }
        if (request.currency() != null) {
            tenant.setCurrency(request.currency());
        }
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.subscriptionPlan() != null) {
            tenant.setSubscriptionPlan(request.subscriptionPlan());
        }
        log.info("Updated tenant id={} status={}", tenant.getId(), tenant.getStatus());
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse rename(Long id, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isBlank()) {
            throw new BusinessException("Organization name is required");
        }
        Tenant tenant = require(id);
        tenant.setName(trimmed);
        log.info("Renamed tenant id={} name={}", tenant.getId(), tenant.getName());
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> listSchools(Long tenantId) {
        require(tenantId);
        return schoolRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(schoolMapper::toResponse)
                .toList();
    }

    @Transactional
    public SchoolResponse addSchool(Long tenantId, CreateSchoolRequest request) {
        Tenant tenant = require(tenantId);
        School school = provisionSchool(tenant, request.name(), request.campusName(),
                request.email(), request.phone(), request.timezone(), request.currency(), request.country());
        return schoolMapper.toResponse(school);
    }

    @Transactional
    public Tenant provisionNewOrganization(RegisterSchoolRequest request) {
        rejectExtraOrganizations();
        String tenantName = blankTo(request.tenantName(), request.schoolName());
        if (tenantName == null || tenantName.isBlank()) {
            throw new BusinessException("Organization name is required");
        }
        return createTenant(
                tenantName,
                request.adminEmail(),
                request.phone(),
                request.country(),
                request.timezone(),
                request.currency());
    }

    @Transactional
    public Tenant ensureDefaultTenant() {
        SmsProperties.Tenancy tenancy = smsProperties.tenancy();
        return tenantRepository.findBySlug(tenancy.defaultTenantSlug()).orElseGet(() -> {
            Tenant tenant = new Tenant();
            tenant.setName(tenancy.defaultTenantName());
            tenant.setSlug(tenancy.defaultTenantSlug());
            tenant.setTimezone("Africa/Dar_es_Salaam");
            tenant.setCurrency("TZS");
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant.setSubscriptionPlan("STARTER");
            tenant = tenantRepository.save(tenant);
            log.info("Ensured default tenant id={} slug={}", tenant.getId(), tenant.getSlug());
            return tenant;
        });
    }

    public Tenant requireDefaultTenant() {
        return tenantRepository.findBySlug(smsProperties.tenancy().defaultTenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Default tenant is not configured"));
    }

    public Tenant require(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
    }

    public School requireSchoolInTenant(Long tenantId, Long schoolId) {
        return schoolRepository.findByIdAndTenantId(schoolId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "School is not part of this tenant"));
    }

    private Tenant createTenant(
            String name,
            String email,
            String phone,
            String country,
            String timezone,
            String currency
    ) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSlug(uniqueTenantSlug(name));
        tenant.setEmail(email);
        tenant.setPhone(phone);
        tenant.setCountry(country);
        tenant.setTimezone(timezone != null && !timezone.isBlank() ? timezone : "Africa/Dar_es_Salaam");
        tenant.setCurrency(currency != null && !currency.isBlank() ? currency : "TZS");
        tenant.setStatus(TenantStatus.TRIAL);
        tenant.setSubscriptionPlan("STARTER");
        tenant.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        tenant = tenantRepository.save(tenant);
        log.info("Created tenant id={} slug={}", tenant.getId(), tenant.getSlug());
        return tenant;
    }

    private School provisionSchool(
            Tenant tenant,
            String schoolName,
            String campusName,
            String email,
            String phone,
            String timezone,
            String currency,
            String country
    ) {
        School school = new School();
        school.setTenantId(tenant.getId());
        school.setName(schoolName);
        school.setSlug(uniqueSchoolSlug(schoolName));
        school.setEmail(email);
        school.setPhone(phone);
        school.setTimezone(timezone != null && !timezone.isBlank() ? timezone : tenant.getTimezone());
        school.setCurrency(currency != null && !currency.isBlank() ? currency : tenant.getCurrency());
        school.setCountry(country != null && !country.isBlank() ? country : tenant.getCountry());
        school.setStatus(SchoolStatus.TRIAL);
        school.setTrialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS));
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setTenantId(tenant.getId());
        campus.setSchoolId(school.getId());
        campus.setName(blankTo(campusName, "Main campus"));
        campus.setCode("MAIN");
        campus.setEmail(email);
        campus.setPhone(phone);
        campus.setTimezone(school.getTimezone());
        campus.setPrimaryCampus(true);
        campusRepository.save(campus);
        log.info("Created school id={} tenantId={} campus={}", school.getId(), tenant.getId(), campus.getName());
        return school;
    }

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getEmail(),
                tenant.getPhone(),
                tenant.getCountry(),
                tenant.getTimezone(),
                tenant.getCurrency(),
                tenant.getStatus(),
                tenant.getSubscriptionPlan(),
                tenant.getTrialEndsAt(),
                schoolRepository.countByTenantId(tenant.getId()));
    }

    private String uniqueTenantSlug(String name) {
        return uniqueSlug(name, tenantRepository::existsBySlug, "tenant");
    }

    private String uniqueSchoolSlug(String name) {
        return uniqueSlug(name, schoolRepository::existsBySlug, "school");
    }

    private String uniqueSlug(String name, java.util.function.Predicate<String> exists, String fallback) {
        String base = SlugUtil.slugify(name);
        if (base.isBlank()) {
            base = fallback;
        }
        String slug = base;
        int attempt = 0;
        while (exists.test(slug)) {
            attempt++;
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (attempt > 8) {
                break;
            }
        }
        return slug;
    }

    private void rejectExtraOrganizations() {
        if (smsProperties.singleTenant()) {
            throw new BusinessException("Creating organizations is disabled in single-tenant mode");
        }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

}
