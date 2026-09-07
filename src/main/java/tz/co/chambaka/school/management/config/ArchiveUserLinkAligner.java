package tz.co.chambaka.school.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate {@code ddl-auto: update} does not drop NOT NULL on existing MySQL columns.
 * Archiving a tenant detaches login users from kept student/teacher/parent rows.
 */
@Component
public class ArchiveUserLinkAligner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArchiveUserLinkAligner.class);

    static final List<String> TABLES = List.of(
            "students",
            "teachers",
            "parents",
            "student_communications"
    );

    static final List<String> COLUMNS = List.of(
            "user_id",
            "user_id",
            "user_id",
            "author_user_id"
    );

    private final JdbcTemplate jdbcTemplate;

    public ArchiveUserLinkAligner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int i = 0; i < TABLES.size(); i++) {
            allowNull(TABLES.get(i), COLUMNS.get(i));
        }
    }

    void allowNull(String table, String column) {
        Integer locked = jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                  and is_nullable = 'NO'
                """,
                Integer.class,
                table,
                column
        );
        if (locked == null || locked == 0) {
            return;
        }
        jdbcTemplate.execute("alter table `" + table + "` modify `" + column + "` bigint null");
        log.info("Allowed {}.{} to be empty so archived accounts can leave live", table, column);
    }
}
