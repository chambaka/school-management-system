package tz.co.chambaka.school.management.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Runs {@link RoleRenameAligner} before Hibernate {@code ddl-auto: update}.
 */
class RoleRenameBeforeJpa implements BeanPostProcessor {

    private boolean done;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (done || !(bean instanceof DataSource dataSource)) {
            return bean;
        }
        done = true;
        RoleRenameAligner.align(jdbcTemplate(dataSource));
        return bean;
    }

    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
