package com.pf4erp.todo.application.port;

import com.pf4erp.todo.domain.Project;
import com.pf4erp.todo.domain.ProjectId;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Optional<Project> findById(ProjectId id);

    List<Project> findAll();

    Project save(Project project);

    void delete(ProjectId id);
}
