package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.tenant.CreateOrganizationAdminRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAdminServiceTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private OrganizationAdminService service;

    @Test
    void listsOrganizationAdmins() {
        User admin = Fixtures.user(8L, Role.ORGANIZATION_ADMIN);
        when(tenantService.require(10L)).thenReturn(Fixtures.tenant());
        when(userRepository.findByTenantIdAndRoleOrderByNameAsc(10L, Role.ORGANIZATION_ADMIN)).thenReturn(List.of(admin));

        var rows = service.list(10L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).email()).isEqualTo(admin.getEmail());
        assertThat(rows.get(0).enabled()).isTrue();
    }

    @Test
    void createsOrganizationAdmin() {
        User admin = Fixtures.user(8L, Role.ORGANIZATION_ADMIN);
        when(tenantService.require(10L)).thenReturn(Fixtures.tenant());
        when(userAccountService.createForTenant(10L, "Asha", "asha@x.com", "HaloCampus1!", Role.ORGANIZATION_ADMIN, "07"))
                .thenReturn(admin);

        var created = service.create(10L, new CreateOrganizationAdminRequest("Asha", "asha@x.com", "HaloCampus1!", "07"));

        assertThat(created.id()).isEqualTo(8L);
        verify(userAccountService).createForTenant(10L, "Asha", "asha@x.com", "HaloCampus1!", Role.ORGANIZATION_ADMIN, "07");
    }

    @Test
    void rejectsWeakPassword() {
        when(tenantService.require(10L)).thenReturn(Fixtures.tenant());
        assertThatThrownBy(() -> service.create(10L, new CreateOrganizationAdminRequest("Asha", "asha@x.com", "weak", "07")))
                .isInstanceOf(BusinessException.class);
    }
}
