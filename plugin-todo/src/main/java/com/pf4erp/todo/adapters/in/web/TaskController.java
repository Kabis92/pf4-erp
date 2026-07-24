package com.pf4erp.todo.adapters.in.web;

import com.pf4erp.todo.application.ProjectUseCase;
import com.pf4erp.todo.application.TaskUseCase;
import com.pf4erp.todo.application.port.EmployeDirectoryPort;
import com.pf4erp.todo.domain.ProjectId;
import com.pf4erp.todo.domain.Task;
import com.pf4erp.todo.domain.TaskId;
import com.pf4erp.todo.domain.TaskStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/todo/tasks")
@Tag(name = "Todo - Tâches")
public class TaskController {

    private final TaskUseCase taskUseCase;
    private final EmployeDirectoryPort employeDirectoryPort;

    public TaskController(TaskUseCase taskUseCase, EmployeDirectoryPort employeDirectoryPort) {
        this.taskUseCase = taskUseCase;
        this.employeDirectoryPort = employeDirectoryPort;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('todo.task.self')")
    public List<TaskDto> mine() {
        try {
            return taskUseCase.listMine(SecuritySupport.currentUserId()).stream().map(this::toDto).toList();
        } catch (TaskUseCase.NoLinkedEmployeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/mine/{id}/status")
    @PreAuthorize("hasAuthority('todo.task.self')")
    public TaskDto updateMyStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request) {
        try {
            return toDto(taskUseCase.updateMyStatus(
                    TaskId.of(id),
                    TaskStatus.from(request.status()),
                    SecuritySupport.currentUserId()
            ));
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (TaskUseCase.TaskNotOwnedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (TaskUseCase.NoLinkedEmployeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public List<TaskDto> list(@RequestParam(required = false) String projectId) {
        List<Task> tasks = projectId == null || projectId.isBlank()
                ? taskUseCase.listAll()
                : taskUseCase.listByProject(ProjectId.of(projectId));
        return tasks.stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto create(@Valid @RequestBody TaskRequest request) {
        try {
            return toDto(taskUseCase.create(
                    ProjectId.of(request.projectId()),
                    request.title(),
                    request.description(),
                    request.assigneeEmployeId(),
                    SecuritySupport.currentUserId()
            ));
        } catch (ProjectUseCase.ProjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto update(@PathVariable String id, @Valid @RequestBody TaskUpdateRequest request) {
        try {
            return toDto(taskUseCase.update(
                    TaskId.of(id),
                    request.title(),
                    request.description(),
                    request.assigneeEmployeId()
            ));
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto updateStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request) {
        try {
            return toDto(taskUseCase.updateStatusAsManager(TaskId.of(id), TaskStatus.from(request.status())));
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public void delete(@PathVariable String id) {
        try {
            taskUseCase.delete(TaskId.of(id));
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private TaskDto toDto(Task task) {
        String assigneeName = task.getAssigneeEmployeId() == null
                ? null
                : employeDirectoryPort.findEmployeNameById(task.getAssigneeEmployeId()).orElse(null);
        return new TaskDto(
                task.getId().getValue(),
                task.getProjectId().getValue(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getAssigneeEmployeId(),
                assigneeName,
                task.getCreatedByUserId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public record TaskRequest(
            @NotBlank String projectId,
            @NotBlank String title,
            String description,
            String assigneeEmployeId
    ) {
    }

    public record TaskUpdateRequest(
            @NotBlank String title,
            String description,
            String assigneeEmployeId
    ) {
    }

    public record StatusRequest(@NotBlank String status) {
    }

    public record TaskDto(
            String id,
            String projectId,
            String title,
            String description,
            String status,
            String assigneeEmployeId,
            String assigneeName,
            String createdByUserId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
