package tz.co.chambaka.school.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate {@code ddl-auto: update} does not drop NOT NULL on existing MySQL columns.
 * Drafts still write a timestamp so inserts succeed if this has not run yet.
 */
@Component
public class AssignmentPublishedAtAligner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AssignmentPublishedAtAligner.class);

    private final JdbcTemplate jdbcTemplate;

    public AssignmentPublishedAtAligner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        allowNull();
    }

    void allowNull() {
        Integer locked = jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.columns
                where table_schema = database()
                  and table_name = 'assignments'
                  and column_name = 'published_at'
                  and is_nullable = 'NO'
                """,
                Integer.class
        );
        if (locked == null || locked == 0) {
            return;
        }
        String columnType = jdbcTemplate.queryForObject(
                """
                select column_type from information_schema.columns
                where table_schema = database()
                  and table_name = 'assignments'
                  and column_name = 'published_at'
                """,
                String.class
        );
        if (columnType == null || columnType.isBlank()) {
            columnType = "datetime(6)";
        }
        jdbcTemplate.execute("alter table `assignments` modify `published_at` " + columnType + " null");
        log.info("Allowed assignments.published_at to be empty for drafts");
    }
}
