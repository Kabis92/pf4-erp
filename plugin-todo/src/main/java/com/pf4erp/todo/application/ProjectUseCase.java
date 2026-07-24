package com.pf4erp.todo.application;

import com.pf4erp.todo.application.port.ProjectRepository;
import com.pf4erp.todo.application.port.TaskRepository;
import com.pf4erp.todo.domain.Project;
import com.pf4erp.todo.domain.ProjectId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class ProjectUseCase {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectUseCase(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public List<Project> list() {
        return projectRepository.findAll();
    }

    public Project get(ProjectId id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    public Project create(String name, String description, String createdByUserId) {
        Project project = new Project(ProjectId.generate(), name, description, createdByUserId, Instant.now());
        return projectRepository.save(project);
    }

    public Project update(ProjectId id, String name, String description) {
        Project existing = get(id);
        return projectRepository.save(existing.with(name, description));
    }

    public void delete(ProjectId id) {
        get(id);
        if (!taskRepository.findByProjectId(id).isEmpty()) {
            throw new ProjectNotEmptyException(id);
        }
        projectRepository.delete(id);
    }

    public static class ProjectNotFoundException extends RuntimeException {
        public ProjectNotFoundException(ProjectId id) {
            super("Projet introuvable: " + id);
        }
    }

    public static class ProjectNotEmptyException extends RuntimeException {
        public ProjectNotEmptyException(ProjectId id) {
            super("Le projet contient encore des tâches: " + id);
        }
    }
}
