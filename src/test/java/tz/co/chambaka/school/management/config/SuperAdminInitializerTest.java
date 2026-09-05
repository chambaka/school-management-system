package tz.co.chambaka.school.management.config;

import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
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
    @InjectMocks
    private SuperAdminInitializer initializer;

    @Test
    void skipsWhenExists() {
        when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(1L);
        initializer.run(new DefaultApplicationArguments());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsWhenMissing() {
        when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(0L);
        when(properties.superAdmin()).thenReturn(Fixtures.properties().superAdmin());
        when(passwordEncoder.encode("ChangeMe123!")).thenReturn("enc");
        initializer.run(new DefaultApplicationArguments());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo("oscar.d@example.net");
    }
}
