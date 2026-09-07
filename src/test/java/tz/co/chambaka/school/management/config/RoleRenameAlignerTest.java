package tz.co.chambaka.school.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleRenameAlignerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private RoleRenameAligner aligner;

    @Test
    void remapsLegacyAdminRoles() {
        when(jdbcTemplate.update(contains("'ADMIN'"))).thenReturn(2);
        when(jdbcTemplate.update(contains("'TENANT_ADMIN'"))).thenReturn(1);
        aligner.run(new DefaultApplicationArguments());
        verify(jdbcTemplate).update(contains("'ADMIN'"));
        verify(jdbcTemplate).update(contains("'TENANT_ADMIN'"));
    }

    @Test
    void skipsLogWhenNothingChanged() {
        when(jdbcTemplate.update(contains("HEADMASTER"))).thenReturn(0);
        aligner.run(new DefaultApplicationArguments());
        verify(jdbcTemplate).update(contains("'ADMIN'"));
    }
}
