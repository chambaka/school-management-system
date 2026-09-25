package tz.co.chambaka.school.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate {@code ddl-auto: update} does not drop NOT NULL on existing MySQL columns.
 * Teacher employee ID is optional; persist {@code null} instead of an empty string.
 */
@Component
public class TeacherEmployeeIdAligner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TeacherEmployeeIdAligner.class);

    private final JdbcTemplate jdbcTemplate;

    public TeacherEmployeeIdAligner(JdbcTemplate jdbcTemplate) {
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
                  and table_name = 'teachers'
                  and column_name = 'employee_id'
                  and is_nullable = 'NO'
                """,
                Integer.class
        );
        if (locked == null || locked == 0) {
            return;
        }
        jdbcTemplate.execute("alter table `teachers` modify `employee_id` varchar(50) null");
        log.info("Allowed teachers.employee_id to be empty");
    }
}
