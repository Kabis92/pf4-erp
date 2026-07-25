package com.pf4erp.todo.application;

import com.pf4erp.todo.application.port.EmployeDirectoryPort;
import com.pf4erp.todo.application.port.ProjectRepository;
import com.pf4erp.todo.application.port.TaskRepository;
import com.pf4erp.todo.domain.ProjectId;
import com.pf4erp.todo.domain.Task;
import com.pf4erp.todo.domain.TaskId;
import com.pf4erp.todo.domain.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class TaskUseCase {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final EmployeDirectoryPort employeDirectoryPort;

    public TaskUseCase(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            EmployeDirectoryPort employeDirectoryPort
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.projectRepository = Objects.requireNonNull(projectRepository);
        this.employeDirectoryPort = Objects.requireNonNull(employeDirectoryPort);
    }

    public List<Task> listAll() {
        return taskRepository.findAll();
    }

    public List<Task> listByProject(ProjectId projectId) {
        ensureProject(projectId);
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> listMine(String userId) {
        String employeId = resolveEmployeId(userId);
        return taskRepository.findByAssigneeEmployeId(employeId);
    }

    public String resolveEmployeId(String userId) {
        return employeDirectoryPort.findEmployeIdByUserId(userId)
                .orElseThrow(NoLinkedEmployeException::new);
    }

    public Task get(TaskId id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task create(
            ProjectId projectId,
            String title,
            String description,
            String assigneeEmployeId,
            String createdByUserId
    ) {
        ensureProject(projectId);
        Task task = new Task(
                TaskId.generate(),
                projectId,
                title,
                description,
                TaskStatus.TODO,
                assigneeEmployeId,
                createdByUserId,
                false,
                false,
                Instant.now(),
                Instant.now()
        );
        return taskRepository.save(task);
    }

    public Task update(TaskId id, String title, String description, String assigneeEmployeId) {
        Task existing = requireActive(get(id));
        return taskRepository.save(existing.withDetails(title, description, assigneeEmployeId));
    }

    public Task updateMyStatus(TaskId id, TaskStatus status, String userId) {
        Task existing = requireActive(get(id));
        String employeId = resolveEmployeId(userId);
        if (!employeId.equals(existing.getAssigneeEmployeId())) {
            throw new TaskNotOwnedException(id);
        }
        return taskRepository.save(existing.withStatus(status));
    }

    public Task updateStatusAsManager(TaskId id, TaskStatus status) {
        Task existing = requireActive(get(id));
        return taskRepository.save(existing.withStatus(status));
    }

    public Task setFavorite(TaskId id, boolean favorite, String userId, boolean manager) {
        Task existing = requireActive(get(id));
        if (!manager) {
            String employeId = resolveEmployeId(userId);
            if (!employeId.equals(existing.getAssigneeEmployeId())) {
                throw new TaskNotOwnedException(id);
            }
        }
        return taskRepository.save(existing.withFavorite(favorite));
    }

    public Task softDelete(TaskId id) {
        Task existing = get(id);
        return taskRepository.save(existing.withDeleted(true));
    }

    public Task restore(TaskId id) {
        Task existing = get(id);
        return taskRepository.save(existing.withDeleted(false));
    }

    public void delete(TaskId id) {
        get(id);
        taskRepository.delete(id);
    }

    private Task requireActive(Task task) {
        if (task.isDeleted()) {
            throw new TaskNotFoundException(task.getId());
        }
        return task;
    }

    private void ensureProject(ProjectId projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectUseCase.ProjectNotFoundException(projectId));
    }

    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(TaskId id) {
            super("Tâche introuvable: " + id);
        }
    }

    public static class TaskNotOwnedException extends RuntimeException {
        public TaskNotOwnedException(TaskId id) {
            super("Cette tâche ne vous est pas assignée: " + id);
        }
    }

    public static class NoLinkedEmployeException extends RuntimeException {
        public NoLinkedEmployeException() {
            super("Aucun employé lié à votre compte — contactez un administrateur RH");
        }
    }
}
