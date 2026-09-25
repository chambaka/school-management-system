package tz.co.chambaka.school.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleRenameAlignerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void remapsLegacyAdminRolesOnUsersAndMessages() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString()))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("column_type"), eq(String.class), anyString(), anyString()))
                .thenReturn("varchar(30)");
        when(jdbcTemplate.update(contains("'ADMIN'"))).thenReturn(2);
        when(jdbcTemplate.update(contains("'TENANT_ADMIN'"))).thenReturn(1);

        new RoleRenameAligner(jdbcTemplate).run(new DefaultApplicationArguments());

        verify(jdbcTemplate, times(2)).update(contains("users"));
        verify(jdbcTemplate, times(2)).update(contains("student_communications"));
        verify(jdbcTemplate, never()).execute(contains("alter table"));
    }

    @Test
    void convertsMessageRoleEnumThenRemaps() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString()))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("column_type"), eq(String.class), anyString(), anyString()))
                .thenReturn("enum('ADMIN','TEACHER')");
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        RoleRenameAligner.align(jdbcTemplate);

        verify(jdbcTemplate, times(2)).execute(contains("varchar(30)"));
        verify(jdbcTemplate).execute(contains("student_communications"));
    }

    @Test
    void skipsMissingTablesAndNullColumnType() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("users")))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("student_communications")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("column_type"), eq(String.class), eq("student_communications"), eq("author_role")))
                .thenReturn(null);
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        RoleRenameAligner.align(jdbcTemplate);

        verify(jdbcTemplate, never()).update(contains("users"));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void tableExistsTreatsNullCountAsMissing() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("users")))
                .thenReturn(null);

        assertThat(RoleRenameAligner.tableExists(jdbcTemplate, "users")).isFalse();
    }

    @Test
    void beforeJpaAlignsOnceWhenDataSourceAppears() {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString()))
                .thenReturn(0);

        RoleRenameBeforeJpa processor = new RoleRenameBeforeJpa() {
            @Override
            JdbcTemplate jdbcTemplate(DataSource dataSource) {
                return RoleRenameAlignerTest.this.jdbcTemplate;
            }
        };

        assertThat(processor.postProcessAfterInitialization("skip", "x")).isEqualTo("skip");
        DataSource first = org.mockito.Mockito.mock(DataSource.class);
        DataSource second = org.mockito.Mockito.mock(DataSource.class);
        assertThat(processor.postProcessAfterInitialization(first, "ds")).isSameAs(first);
        assertThat(processor.postProcessAfterInitialization(second, "ds2")).isSameAs(second);
        verify(jdbcTemplate, times(4)).queryForObject(contains("information_schema.tables"), eq(Integer.class), anyString());
        assertThat(RoleRenameEarlyConfig.roleRenameBeforeJpa()).isInstanceOf(RoleRenameBeforeJpa.class);
        assertThat(new RoleRenameBeforeJpa().jdbcTemplate(org.mockito.Mockito.mock(DataSource.class))).isNotNull();
    }
}
