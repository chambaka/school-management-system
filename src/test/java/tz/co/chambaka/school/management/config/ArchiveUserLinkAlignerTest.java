package tz.co.chambaka.school.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveUserLinkAlignerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private ArchiveUserLinkAligner aligner;

    @Test
    void altersNotNullProfileLinksThenSkipsAlreadyNullable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                .thenReturn(1, 0, 1, 0);

        aligner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, times(2)).execute(contains("alter table"));
        verify(jdbcTemplate).execute(contains("`students`"));
        verify(jdbcTemplate).execute(contains("`parents`"));
        verify(jdbcTemplate, never()).execute(contains("`teachers`"));
        verify(jdbcTemplate, never()).execute(contains("`student_communications`"));
    }

    @Test
    void allowNullIgnoresMissingCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("students"), eq("user_id")))
                .thenReturn(null);

        aligner.allowNull("students", "user_id");

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
