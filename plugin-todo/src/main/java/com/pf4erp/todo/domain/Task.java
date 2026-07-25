package com.pf4erp.todo.domain;

import java.time.Instant;
import java.util.Objects;

public final class Task {

    private final TaskId id;
    private final ProjectId projectId;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final String assigneeEmployeId;
    private final String createdByUserId;
    private final boolean favorite;
    private final boolean deleted;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Task(
            TaskId id,
            ProjectId projectId,
            String title,
            String description,
            TaskStatus status,
            String assigneeEmployeId,
            String createdByUserId,
            boolean favorite,
            boolean deleted,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.projectId = Objects.requireNonNull(projectId);
        this.title = requireText(title, "title");
        this.description = description != null ? description.trim() : "";
        this.status = status != null ? status : TaskStatus.TODO;
        this.assigneeEmployeId = blankToNull(assigneeEmployeId);
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.favorite = favorite;
        this.deleted = deleted;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public Task withDetails(String title, String description, String assigneeEmployeId) {
        return copy(title, description, status, assigneeEmployeId, favorite, deleted);
    }

    public Task withStatus(TaskStatus status) {
        return copy(title, description, status, assigneeEmployeId, favorite, deleted);
    }

    public Task withFavorite(boolean favorite) {
        return copy(title, description, status, assigneeEmployeId, favorite, deleted);
    }

    public Task withDeleted(boolean deleted) {
        return copy(title, description, status, assigneeEmployeId, favorite, deleted);
    }

    private Task copy(
            String title,
            String description,
            TaskStatus status,
            String assigneeEmployeId,
            boolean favorite,
            boolean deleted
    ) {
        return new Task(
                id, projectId, title, description, status, assigneeEmployeId,
                createdByUserId, favorite, deleted, createdAt, Instant.now()
        );
    }

    public TaskId getId() { return id; }
    public ProjectId getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public String getAssigneeEmployeId() { return assigneeEmployeId; }
    public String getCreatedByUserId() { return createdByUserId; }
    public boolean isFavorite() { return favorite; }
    public boolean isDeleted() { return deleted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
