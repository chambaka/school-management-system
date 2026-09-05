package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserAccountService service;

    @Test
    void createsLowercasedEmail() {
        when(userRepository.existsByEmailIgnoreCase("A@B.COM")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(1L, "N", "A@B.COM", "pw", Role.TEACHER, "07");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    void duplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, "N", "a@b.com", "pw", Role.STUDENT, null))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
