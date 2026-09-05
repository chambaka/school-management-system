package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.school.CreateSchoolRequest;
import tz.co.chambaka.school.management.dto.tenant.CreateTenantRequest;
import tz.co.chambaka.school.management.dto.tenant.UpdateTenantRequest;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.model.Campus;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.enums.TenantStatus;
import tz.co.chambaka.school.management.repository.CampusRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private CampusRepository campusRepository;
    @Mock
    private SchoolMapper schoolMapper;
    @InjectMocks
    private TenantService tenantService;

    @Test
    void listGetCreateAndUpdate() {
        Tenant tenant = Fixtures.tenant();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
        when(schoolRepository.countByTenantId(10L)).thenReturn(2L);
        when(tenantRepository.existsBySlug("new-group")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        assertThat(tenantService.list()).hasSize(1);
        assertThat(tenantService.get(10L).schoolCount()).isEqualTo(2L);
        assertThat(tenantService.create(new CreateTenantRequest("New Group", "a@b.com", "07", "TZ", "UTC", "USD"))
                .timezone()).isEqualTo("UTC");

        tenantService.update(10L, new UpdateTenantRequest(
                "Renamed", "e@e.com", "08", "KE", "UTC", "USD", TenantStatus.ACTIVE, "PRO"));
        assertThat(tenant.getName()).isEqualTo("Renamed");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getSubscriptionPlan()).isEqualTo("PRO");
    }

    @Test
    void createUsesDefaultTimezoneCurrency() {
        when(tenantRepository.existsBySlug(any())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolRepository.countByTenantId(null)).thenReturn(0L);
        var response = tenantService.create(new CreateTenantRequest("Org", null, null, null, "  ", ""));
        assertThat(response.timezone()).isEqualTo("Africa/Dar_es_Salaam");
        assertThat(response.currency()).isEqualTo("TZS");
    }

    @Test
    void addSchoolAndListSchools() {
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(Fixtures.tenant()));
        when(schoolRepository.existsBySlug("east-campus-school")).thenReturn(false);
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> {
            School school = inv.getArgument(0);
            school.setId(3L);
            return school;
        });
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolMapper.toResponse(any(School.class))).thenReturn(Fixtures.schoolResponse());
        when(schoolRepository.findByTenantIdOrderByNameAsc(10L)).thenReturn(List.of(Fixtures.school()));

        assertThat(tenantService.addSchool(10L, new CreateSchoolRequest(
                "East Campus School", "East", "e@e.com", "07", "UTC", "USD", "TZ")).id()).isEqualTo(1L);
        assertThat(tenantService.listSchools(10L)).hasSize(1);
    }

    @Test
    void provisionNewOrganization() {
        stubSaves();
        when(tenantRepository.existsBySlug("chambaka-group")).thenReturn(true, false);
        when(schoolRepository.existsBySlug("chambaka-secondary")).thenReturn(false);
        when(campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(1L))
                .thenReturn(Optional.of(Fixtures.campus()));
        var org = tenantService.provisionNewOrganization(new RegisterSchoolRequest(
                "Chambaka Secondary", "a@b.com", "HaloCampus1!", "A", "07", null, null, "TZ",
                "Chambaka Group", null));
        assertThat(org.tenant().getId()).isEqualTo(10L);
        assertThat(org.school().getTenantId()).isEqualTo(10L);
        assertThat(org.campus().getCode()).isEqualTo("MAIN");
    }

    @Test
    void requireSchoolInTenant() {
        when(schoolRepository.findByIdAndTenantId(1L, 10L)).thenReturn(Optional.of(Fixtures.school()));
        assertThat(tenantService.requireSchoolInTenant(10L, 1L).getId()).isEqualTo(1L);
        when(schoolRepository.findByIdAndTenantId(2L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantService.requireSchoolInTenant(10L, 2L))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void requireMissing() {
        when(tenantRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantService.require(8L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void provisionMissingPrimaryCampus() {
        stubSaves();
        when(tenantRepository.existsBySlug(any())).thenReturn(false);
        when(schoolRepository.existsBySlug(any())).thenReturn(false);
        when(campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantService.provisionNewOrganization(new RegisterSchoolRequest(
                "X", "a@b.com", "HaloCampus1!", "A", null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubSaves() {
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant tenant = inv.getArgument(0);
            tenant.setId(10L);
            return tenant;
        });
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> {
            School school = inv.getArgument(0);
            school.setId(1L);
            return school;
        });
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}
