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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentPublishedAtAlignerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private AssignmentPublishedAtAligner aligner;

    @Test
    void altersNotNullPublishedAtThenSkipsAlreadyNullable() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("datetime(6)");
        aligner.run(new DefaultApplicationArguments());
        verify(jdbcTemplate).execute(contains("alter table `assignments` modify `published_at` datetime(6) null"));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        aligner.run(new DefaultApplicationArguments());
        verify(jdbcTemplate).execute(anyString());
    }

    @Test
    void allowNullIgnoresMissingCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);
        aligner.allowNull();
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void allowNullUsesDatetimeWhenColumnTypeMissing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn(" ");
        aligner.allowNull();
        verify(jdbcTemplate).execute(contains("datetime(6) null"));
    }
}
