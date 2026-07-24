package com.pf4erp.todo.application.port;

import com.pf4erp.todo.domain.ProjectId;
import com.pf4erp.todo.domain.Task;
import com.pf4erp.todo.domain.TaskId;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Optional<Task> findById(TaskId id);

    List<Task> findByProjectId(ProjectId projectId);

    List<Task> findByAssigneeEmployeId(String employeId);

    List<Task> findAll();

    Task save(Task task);

    void delete(TaskId id);
}
