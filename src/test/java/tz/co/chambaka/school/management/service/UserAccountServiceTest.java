package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private EntityManager entityManager;
    @InjectMocks
    private UserAccountService service;

    @Test
    void createsLowercasedEmailAndTenant() {
        when(userRepository.existsByEmailIgnoreCase("A@B.COM")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(1L, "N", "A@B.COM", "pw", Role.TEACHER, "0753493500");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.TEACHER);
        assertThat(captor.getValue().getPhone()).isEqualTo("255753493500");
        assertThat(captor.getValue().getTenantId()).isEqualTo(Fixtures.TENANT_ID);
        assertThat(captor.getValue().getSchoolId()).isEqualTo(1L);
    }

    @Test
    void createLeavesTenantBlankWhenSchoolMissing() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(1L, "N", "a@b.com", "pw", Role.STUDENT, null);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isNull();
    }

    @Test
    void duplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, "N", "a@b.com", "pw", Role.STUDENT, null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteForTenantRemovesSchoolAndTenantUsersButKeepsPlatformAdmin() {
        stubDetachQueries();
        User tenantAdmin = Fixtures.user(2L, Role.TENANT_ADMIN);
        User schoolTeacher = Fixtures.user(3L, Role.TEACHER);
        User platform = Fixtures.user(1L, Role.SUPER_ADMIN);
        when(userRepository.findByTenantId(10L)).thenReturn(List.of(tenantAdmin, platform));
        when(userRepository.findBySchoolIdIn(List.of(1L))).thenReturn(List.of(schoolTeacher, tenantAdmin));

        service.deleteForTenant(10L, List.of(1L));

        verify(userRepository).deleteAllById(List.of(2L, 3L));
        verify(entityManager, times(10)).createQuery(anyString());
    }

    @Test
    void createWithoutSchoolLeavesTenantBlank() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(null, "N", "a@b.com", "pw", Role.ADMIN, null);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isNull();
        assertThat(captor.getValue().getSchoolId()).isNull();
    }

    @Test
    void deleteForTenantSkipsSchoolLookupWhenNoSchools() {
        stubDetachQueries();
        when(userRepository.findByTenantId(10L)).thenReturn(List.of(Fixtures.user(2L, Role.TENANT_ADMIN)));

        service.deleteForTenant(10L, List.of());
        service.deleteForTenant(10L, null);

        verify(userRepository, never()).findBySchoolIdIn(any());
        verify(userRepository, times(2)).deleteAllById(List.of(2L));
    }

    @Test
    void deleteForSchoolRemovesSchoolUsers() {
        stubDetachQueries();
        when(userRepository.findBySchoolId(1L)).thenReturn(List.of(Fixtures.user(4L, Role.ADMIN)));

        service.deleteForSchool(1L);

        verify(userRepository).deleteAllById(List.of(4L));
    }

    @Test
    void deleteDoesNothingWhenOnlyPlatformAdminRemains() {
        when(userRepository.findBySchoolId(1L)).thenReturn(List.of(Fixtures.user(1L, Role.SUPER_ADMIN)));

        service.deleteForSchool(1L);

        verify(entityManager, never()).createQuery(anyString());
        verify(userRepository, never()).deleteAllById(any());
    }

    private void stubDetachQueries() {
        Query query = org.mockito.Mockito.mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);
    }
}
