package tz.co.chambaka.school.management.security;

import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadsByEmail() {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(Fixtures.user(1L, Role.ADMIN)));
        var details = service.loadUserByUsername("admin@example.com");
        assertThat(details).isInstanceOf(UserPrincipal.class);
        assertThat(details.getUsername()).isEqualTo("admin@example.com");
    }

    @Test
    void missingUser() {
        when(userRepository.findByEmailIgnoreCase("none@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("none@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
