package tz.co.chambaka.school.management.security;

import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void mapsUserDetails() {
        User user = Fixtures.user(2L, Role.TEACHER);
        UserPrincipal principal = new UserPrincipal(user);
        assertThat(principal.getId()).isEqualTo(2L);
        assertThat(principal.getTenantId()).isEqualTo(10L);
        assertThat(principal.getSchoolId()).isEqualTo(1L);
        assertThat(principal.getCampusId()).isEqualTo(20L);
        assertThat(principal.getUsername()).isEqualTo(user.getEmail());
        assertThat(principal.getPassword()).isEqualTo("hashed");
        assertThat(principal.getRole()).isEqualTo(Role.TEACHER);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_TEACHER");
    }
}
