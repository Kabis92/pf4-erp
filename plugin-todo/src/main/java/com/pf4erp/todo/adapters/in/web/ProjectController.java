package com.pf4erp.todo.adapters.in.web;

import com.pf4erp.todo.application.ProjectUseCase;
import com.pf4erp.todo.domain.Project;
import com.pf4erp.todo.domain.ProjectId;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/todo/projects")
@Tag(name = "Todo - Projets")
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    public ProjectController(ProjectUseCase projectUseCase) {
        this.projectUseCase = projectUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public List<ProjectDto> list() {
        return projectUseCase.list().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public ProjectDto get(@PathVariable String id) {
        try {
            return toDto(projectUseCase.get(ProjectId.of(id)));
        } catch (ProjectUseCase.ProjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public ProjectDto create(@Valid @RequestBody ProjectRequest request) {
        return toDto(projectUseCase.create(
                request.name(),
                request.description(),
                SecuritySupport.currentUserId()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public ProjectDto update(@PathVariable String id, @Valid @RequestBody ProjectRequest request) {
        try {
            return toDto(projectUseCase.update(ProjectId.of(id), request.name(), request.description()));
        } catch (ProjectUseCase.ProjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public void delete(@PathVariable String id) {
        try {
            projectUseCase.delete(ProjectId.of(id));
        } catch (ProjectUseCase.ProjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (ProjectUseCase.ProjectNotEmptyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId().getValue(),
                project.getName(),
                project.getDescription(),
                project.getCreatedByUserId(),
                project.getCreatedAt()
        );
    }

    public record ProjectRequest(
            @NotBlank String name,
            String description
    ) {
    }

    public record ProjectDto(
            String id,
            String name,
            String description,
            String createdByUserId,
            Instant createdAt
    ) {
    }
}
