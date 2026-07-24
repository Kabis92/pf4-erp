package com.pf4erp.todo.config;

import com.pf4erp.todo.adapters.in.web.ProjectController;
import com.pf4erp.todo.adapters.in.web.TaskController;
import com.pf4erp.todo.adapters.out.persistence.mongo.MongoProjectRepository;
import com.pf4erp.todo.adapters.out.persistence.mongo.MongoTaskRepository;
import com.pf4erp.todo.adapters.out.rh.MongoEmployeDirectoryAdapter;
import com.pf4erp.todo.application.ProjectUseCase;
import com.pf4erp.todo.application.TaskUseCase;
import com.pf4erp.todo.application.port.EmployeDirectoryPort;
import com.pf4erp.todo.application.port.ProjectRepository;
import com.pf4erp.todo.application.port.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class TodoPluginConfiguration {

    @Bean
    public ProjectRepository projectRepository(MongoTemplate mongoTemplate) {
        return new MongoProjectRepository(mongoTemplate);
    }

    @Bean
    public TaskRepository taskRepository(MongoTemplate mongoTemplate) {
        return new MongoTaskRepository(mongoTemplate);
    }

    @Bean
    public EmployeDirectoryPort employeDirectoryPort(MongoTemplate mongoTemplate) {
        return new MongoEmployeDirectoryAdapter(mongoTemplate);
    }

    @Bean
    public ProjectUseCase projectUseCase(ProjectRepository projectRepository, TaskRepository taskRepository) {
        return new ProjectUseCase(projectRepository, taskRepository);
    }

    @Bean
    public TaskUseCase taskUseCase(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            EmployeDirectoryPort employeDirectoryPort
    ) {
        return new TaskUseCase(taskRepository, projectRepository, employeDirectoryPort);
    }

    @Bean
    public ProjectController projectController(ProjectUseCase projectUseCase) {
        return new ProjectController(projectUseCase);
    }

    @Bean
    public TaskController taskController(TaskUseCase taskUseCase, EmployeDirectoryPort employeDirectoryPort) {
        return new TaskController(taskUseCase, employeDirectoryPort);
    }
}
