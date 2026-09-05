package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.dto.school.UpdateSchoolRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchoolService {

    private static final Logger log = LoggerFactory.getLogger(SchoolService.class);

    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    public SchoolService(SchoolRepository schoolRepository, SchoolMapper schoolMapper) {
        this.schoolRepository = schoolRepository;
        this.schoolMapper = schoolMapper;
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> list() {
        return schoolRepository.findAll().stream().map(schoolMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SchoolResponse get(Long id) {
        return schoolMapper.toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public BrandingResponse brandingBySlug(String slug) {
        School school = schoolRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("School not found for slug: " + slug));
        return schoolMapper.toBranding(school);
    }

    @Transactional(readOnly = true)
    public BrandingResponse brandingByHost(String host) {
        return schoolRepository.findByCustomDomain(host)
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
            school.setPhone(request.phone());
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
            school.setStatus(request.status());
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
        return schoolMapper.toResponse(school);
    }

    public School require(Long id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("School", id));
    }
}
