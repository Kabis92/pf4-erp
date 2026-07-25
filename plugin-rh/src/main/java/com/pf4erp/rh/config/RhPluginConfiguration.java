package com.pf4erp.rh.config;

import com.pf4erp.rh.adapters.in.web.EmployeController;
import com.pf4erp.rh.adapters.out.api.SpringEmployeDirectory;
import com.pf4erp.rh.adapters.out.persistence.mongo.MongoEmployeRepository;
import com.pf4erp.rh.api.EmployeDirectory;
import com.pf4erp.rh.application.EmployeUseCase;
import com.pf4erp.rh.application.port.EmployeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class RhPluginConfiguration {

    @Bean
    public EmployeRepository employeRepository(MongoTemplate mongoTemplate) {
        return new MongoEmployeRepository(mongoTemplate);
    }

    @Bean
    public EmployeUseCase employeUseCase(
            EmployeRepository employeRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        return new EmployeUseCase(employeRepository, eventPublisher);
    }

    /** Extension point RH — découvrable via ExtensionGetter depuis les autres plugins. */
    @Bean
    public EmployeDirectory employeDirectory(EmployeRepository employeRepository) {
        return new SpringEmployeDirectory(employeRepository);
    }

    @Bean
    public EmployeController employeController(EmployeUseCase employeUseCase) {
        return new EmployeController(employeUseCase);
    }

    @Bean
    public RhDemoDataSeeder rhDemoDataSeeder(MongoTemplate mongoTemplate) {
        return new RhDemoDataSeeder(mongoTemplate);
    }
}
