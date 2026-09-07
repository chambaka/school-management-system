package tz.co.chambaka.school.management.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RoleRenameEarlyConfig {

    @Bean
    static BeanPostProcessor roleRenameBeforeJpa() {
        return new RoleRenameBeforeJpa();
    }
}
