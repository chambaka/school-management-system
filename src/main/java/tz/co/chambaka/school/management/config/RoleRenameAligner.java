package tz.co.chambaka.school.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
        int admins = jdbcTemplate.update("update users set role = 'HEADMASTER' where role = 'ADMIN'");
        int tenants = jdbcTemplate.update("update users set role = 'HEADMASTER' where role = 'TENANT_ADMIN'");
        if (admins + tenants > 0) {
            log.info("Renamed legacy roles to HEADMASTER adminRows={} tenantAdminRows={}", admins, tenants);
        }
    }
}
