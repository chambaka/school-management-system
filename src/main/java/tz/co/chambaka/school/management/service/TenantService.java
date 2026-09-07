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
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import tz.co.chambaka.school.management.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
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
    private final UserAccountService userAccountService;
    private final DepartmentService departmentService;

    public TenantService(
            TenantRepository tenantRepository,
            SchoolRepository schoolRepository,
            CampusRepository campusRepository,
            SchoolMapper schoolMapper,
            SmsProperties smsProperties,
            UserAccountService userAccountService,
            DepartmentService departmentService
    ) {
        this.tenantRepository = tenantRepository;
        this.schoolRepository = schoolRepository;
        this.campusRepository = campusRepository;
        this.schoolMapper = schoolMapper;
        this.smsProperties = smsProperties;
        this.userAccountService = userAccountService;
        this.departmentService = departmentService;
    }

    @PostConstruct
    @Transactional
    public void activateLegacyTrials() {
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() == TenantStatus.TRIAL) {
                tenant.setStatus(TenantStatus.ACTIVE);
                tenant.setTrialEndsAt(null);
            }
        }
        for (School school : schoolRepository.findAll()) {
            if (school.getStatus() == SchoolStatus.TRIAL) {
                school.setStatus(SchoolStatus.ACTIVE);
                school.setTrialEndsAt(null);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getStatus() != TenantStatus.ARCHIVED)
                .map(this::toResponse)
                .toList();
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
            tenant.setPhone(PhoneNumbers.persist(request.phone()));
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
            if (request.status() == TenantStatus.ARCHIVED) {
                throw new BusinessException("Use delete to archive an organization");
            }
            if (request.status() == TenantStatus.TRIAL) {
                tenant.setStatus(TenantStatus.ACTIVE);
            } else {
                tenant.setStatus(request.status());
            }
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
        Tenant tenant = require(tenantId);
        return schoolRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .filter(school -> school.getStatus() != SchoolStatus.ARCHIVED)
                .map(school -> schoolMapper.toResponse(school).withTenantName(tenant.getName()))
                .toList();
    }

    @Transactional
    public SchoolResponse addSchool(Long tenantId, CreateSchoolRequest request) {
        Tenant tenant = require(tenantId);
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new BusinessException("Schools can only be added to an active organization");
        }
        School school = provisionSchool(tenant, request.name(),
                request.email(), request.phone(), request.timezone(), request.currency(), request.country());
        return schoolMapper.toResponse(school);
    }

    @Transactional
    public void archive(Long id) {
        Tenant tenant = require(id);
        tenant.setStatus(TenantStatus.ARCHIVED);
        tenant.setTrialEndsAt(null);
        List<Long> schoolIds = new ArrayList<>();
        for (School school : schoolRepository.findByTenantIdOrderByNameAsc(id)) {
            school.setStatus(SchoolStatus.ARCHIVED);
            school.setTrialEndsAt(null);
            schoolIds.add(school.getId());
        }
        userAccountService.deleteForTenant(id, schoolIds);
        log.info("Archived tenant id={} and removed it from live", tenant.getId());
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
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", id));
        if (tenant.getStatus() == TenantStatus.ARCHIVED) {
            throw ResourceNotFoundException.of("Tenant", id);
        }
        return tenant;
    }

    public School requireSchoolInTenant(Long tenantId, Long schoolId) {
        School school = schoolRepository.findByIdAndTenantId(schoolId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "School is not part of this tenant"));
        if (school.getStatus() == SchoolStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "School is not part of this tenant");
        }
        return school;
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
        tenant.setPhone(PhoneNumbers.persist(phone));
        tenant.setCountry(country);
        tenant.setTimezone(timezone != null && !timezone.isBlank() ? timezone : "Africa/Dar_es_Salaam");
        tenant.setCurrency(currency != null && !currency.isBlank() ? currency : "TZS");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSubscriptionPlan("STARTER");
        tenant.setTrialEndsAt(null);
        tenant = tenantRepository.save(tenant);
        log.info("Created tenant id={} slug={}", tenant.getId(), tenant.getSlug());
        return tenant;
    }

    private School provisionSchool(
            Tenant tenant,
            String schoolName,
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
        school.setPhone(PhoneNumbers.persist(phone));
        school.setTimezone(timezone != null && !timezone.isBlank() ? timezone : tenant.getTimezone());
        school.setCurrency(currency != null && !currency.isBlank() ? currency : tenant.getCurrency());
        school.setCountry(country != null && !country.isBlank() ? country : tenant.getCountry());
        school.setStatus(SchoolStatus.ACTIVE);
        school.setTrialEndsAt(null);
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setTenantId(tenant.getId());
        campus.setSchoolId(school.getId());
        campus.setName(schoolName);
        campus.setCode("MAIN");
        campus.setEmail(email);
        campus.setPhone(PhoneNumbers.persist(phone));
        campus.setTimezone(school.getTimezone());
        campus.setPrimaryCampus(true);
        campusRepository.save(campus);
        departmentService.ensureDefaults(school.getId());
        log.info("Created school id={} tenantId={}", school.getId(), tenant.getId());
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
                schoolRepository.findByTenantIdOrderByNameAsc(tenant.getId()).stream()
                        .filter(school -> school.getStatus() != SchoolStatus.ARCHIVED)
                        .count());
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
