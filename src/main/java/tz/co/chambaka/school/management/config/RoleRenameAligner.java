package tz.co.chambaka.school.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Legacy {@code ADMIN} / {@code TENANT_ADMIN} must become {@code HEADMASTER} before Hibernate
 * shrinks MySQL enums. {@link RoleRenameBeforeJpa} runs this as soon as the DataSource exists.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RoleRenameAligner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleRenameAligner.class);

    private final JdbcTemplate jdbcTemplate;

    public RoleRenameAligner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        align(jdbcTemplate);
    }

    static void align(JdbcTemplate jdbc) {
        widenRoleColumnIfEnum(jdbc, "users", "role");
        remapIfPresent(jdbc, "users", "role");
        widenRoleColumnIfEnum(jdbc, "student_communications", "author_role");
        remapIfPresent(jdbc, "student_communications", "author_role");
    }

    static void remapIfPresent(JdbcTemplate jdbc, String table, String column) {
        if (!tableExists(jdbc, table)) {
            return;
        }
        int admins = jdbc.update(
                "update `" + table + "` set `" + column + "` = 'HEADMASTER' where `" + column + "` = 'ADMIN'");
        int tenants = jdbc.update(
                "update `" + table + "` set `" + column + "` = 'HEADMASTER' where `" + column + "` = 'TENANT_ADMIN'");
        if (admins + tenants > 0) {
            log.info("Renamed legacy roles to HEADMASTER table={} adminRows={} tenantAdminRows={}",
                    table, admins, tenants);
        }
    }

    static void widenRoleColumnIfEnum(JdbcTemplate jdbc, String table, String column) {
        if (!tableExists(jdbc, table)) {
            return;
        }
        String type = jdbc.queryForObject(
                """
                select column_type from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """,
                String.class,
                table,
                column
        );
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("enum")) {
            return;
        }
        jdbc.execute("alter table `" + table + "` modify `" + column + "` varchar(30) not null");
        log.info("Changed {}.{} from enum to varchar so role names can change", table, column);
    }

    static boolean tableExists(JdbcTemplate jdbc, String table) {
        Integer found = jdbc.queryForObject(
                """
                select count(*) from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """,
                Integer.class,
                table
        );
        return found != null && found > 0;
    }
}
