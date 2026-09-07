package tz.co.chambaka.school.management.config;

import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.service.TenantService;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SmsProperties properties;
    @Mock
    private TenantService tenantService;
    @InjectMocks
    private SuperAdminInitializer initializer;

    @Test
    void skipsWhenExists() {
        when(userRepository.findByRole(Role.SUPER_ADMIN)).thenReturn(List.of(Fixtures.user(1L, Role.SUPER_ADMIN)));
        when(properties.superAdmin()).thenReturn(Fixtures.properties().superAdmin());
        initializer.run(new DefaultApplicationArguments());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsWhenMissing() {
        when(userRepository.findByRole(Role.SUPER_ADMIN)).thenReturn(List.of());
        when(properties.superAdmin()).thenReturn(Fixtures.properties().superAdmin());
        when(passwordEncoder.encode("ChangeMe123!")).thenReturn("enc");
        initializer.run(new DefaultApplicationArguments());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo("halo.admin@halo-schools.net");
    }

    @Test
    void renamesLegacySeedEmail() {
        User admin = Fixtures.user(1L, Role.SUPER_ADMIN);
        admin.setEmail("oscar.d@example.net");
        when(userRepository.findByRole(Role.SUPER_ADMIN)).thenReturn(List.of(admin));
        when(properties.superAdmin()).thenReturn(Fixtures.properties().superAdmin());
        initializer.run(new DefaultApplicationArguments());
        assertThat(admin.getEmail()).isEqualTo("halo.admin@halo-schools.net");
        verify(userRepository).save(admin);
    }

    @Test
    void bindsSuperAdminToDefaultTenantInSingleMode() {
        User admin = Fixtures.user(1L, Role.SUPER_ADMIN);
        when(userRepository.findByRole(Role.SUPER_ADMIN)).thenReturn(List.of(admin));
        when(properties.superAdmin()).thenReturn(Fixtures.properties().superAdmin());
        when(properties.singleTenant()).thenReturn(true);
        when(tenantService.ensureDefaultTenant()).thenReturn(Fixtures.tenant());
        initializer.run(new DefaultApplicationArguments());
        assertThat(admin.getTenantId()).isEqualTo(Fixtures.TENANT_ID);
        verify(userRepository).save(admin);
    }

    @Test
    void skipsBindWhenSuperAdminAlreadyHasTenant() {
        User admin = Fixtures.user(1L, Role.SUPER_ADMIN);
        admin.setTenantId(10L);
        when(userRepository.findByRole(Role.SUPER_ADMIN)).thenReturn(List.of(admin));
        when(properties.superAdmin()).thenReturn(Fixtures.properties().superAdmin());
        when(properties.singleTenant()).thenReturn(true);
        when(tenantService.ensureDefaultTenant()).thenReturn(Fixtures.tenant());
        initializer.run(new DefaultApplicationArguments());
        verify(userRepository, never()).save(any());
    }
}
