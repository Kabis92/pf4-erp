package com.pf4erp.todo.adapters.in.web;

import com.pf4erp.todo.application.ProjectUseCase;
import com.pf4erp.todo.application.TaskUseCase;
import com.pf4erp.todo.application.port.EmployeDirectoryPort;
import com.pf4erp.todo.application.port.ProjectRepository;
import com.pf4erp.todo.domain.Project;
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
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/todo/tasks")
@Tag(name = "Todo - Tâches")
public class TaskController {

    private final TaskUseCase taskUseCase;
    private final EmployeDirectoryPort employeDirectoryPort;
    private final ProjectRepository projectRepository;

    public TaskController(
            TaskUseCase taskUseCase,
            EmployeDirectoryPort employeDirectoryPort,
            ProjectRepository projectRepository
    ) {
        this.taskUseCase = taskUseCase;
        this.employeDirectoryPort = employeDirectoryPort;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('todo.task.self')")
    public List<TaskDto> mine() {
        try {
            return mapTasks(taskUseCase.listMine(SecuritySupport.currentUserId()));
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
            ), projectNames());
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

    @PutMapping("/mine/{id}/favorite")
    @PreAuthorize("hasAuthority('todo.task.self')")
    public TaskDto updateMyFavorite(@PathVariable String id, @Valid @RequestBody FavoriteRequest request) {
        try {
            return toDto(taskUseCase.setFavorite(
                    TaskId.of(id),
                    request.favorite(),
                    SecuritySupport.currentUserId(),
                    false
            ), projectNames());
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (TaskUseCase.TaskNotOwnedException | TaskUseCase.NoLinkedEmployeException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public List<TaskDto> list(@RequestParam(required = false) String projectId) {
        List<Task> tasks = projectId == null || projectId.isBlank()
                ? taskUseCase.listAll()
                : taskUseCase.listByProject(ProjectId.of(projectId));
        return mapTasks(tasks);
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
            ), projectNames());
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
            ), projectNames());
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto updateStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request) {
        try {
            return toDto(taskUseCase.updateStatusAsManager(TaskId.of(id), TaskStatus.from(request.status())), projectNames());
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }
    }

    @PutMapping("/{id}/favorite")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto updateFavorite(@PathVariable String id, @Valid @RequestBody FavoriteRequest request) {
        try {
            return toDto(taskUseCase.setFavorite(
                    TaskId.of(id),
                    request.favorite(),
                    SecuritySupport.currentUserId(),
                    true
            ), projectNames());
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}/trash")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto trash(@PathVariable String id) {
        try {
            return toDto(taskUseCase.softDelete(TaskId.of(id)), projectNames());
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public TaskDto restore(@PathVariable String id) {
        try {
            return toDto(taskUseCase.restore(TaskId.of(id)), projectNames());
        } catch (TaskUseCase.TaskNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
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

    private List<TaskDto> mapTasks(List<Task> tasks) {
        Map<String, String> names = projectNames();
        return tasks.stream().map(t -> toDto(t, names)).toList();
    }

    private Map<String, String> projectNames() {
        return projectRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId().getValue(), Project::getName, (a, b) -> a));
    }

    private TaskDto toDto(Task task, Map<String, String> projectNames) {
        String assigneeName = task.getAssigneeEmployeId() == null
                ? null
                : employeDirectoryPort.findEmployeNameById(task.getAssigneeEmployeId()).orElse(null);
        return new TaskDto(
                task.getId().getValue(),
                task.getProjectId().getValue(),
                projectNames.getOrDefault(task.getProjectId().getValue(), "Project"),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getAssigneeEmployeId(),
                assigneeName,
                task.isFavorite(),
                task.isDeleted(),
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

    public record FavoriteRequest(boolean favorite) {
    }

    public record TaskDto(
            String id,
            String projectId,
            String projectName,
            String title,
            String description,
            String status,
            String assigneeEmployeId,
            String assigneeName,
            boolean favorite,
            boolean deleted,
            String createdByUserId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
