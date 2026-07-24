package com.pf4erp.rh.config;

import com.pf4erp.rh.adapters.in.web.EmployeController;
import com.pf4erp.rh.adapters.out.persistence.mongo.MongoEmployeRepository;
import com.pf4erp.rh.application.EmployeUseCase;
import com.pf4erp.rh.application.port.EmployeRepository;
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
    public EmployeUseCase employeUseCase(EmployeRepository employeRepository) {
        return new EmployeUseCase(employeRepository);
    }

    @Bean
    public EmployeController employeController(EmployeUseCase employeUseCase) {
        return new EmployeController(employeUseCase);
    }
}
