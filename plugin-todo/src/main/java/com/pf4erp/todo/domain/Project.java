package com.pf4erp.todo.domain;

import java.time.Instant;
import java.util.Objects;

public final class Project {

    private final ProjectId id;
    private final String name;
    private final String description;
    private final String createdByUserId;
    private final Instant createdAt;

    public Project(ProjectId id, String name, String description, String createdByUserId, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.name = requireText(name, "name");
        this.description = description != null ? description.trim() : "";
        this.createdByUserId = Objects.requireNonNull(createdByUserId);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Project with(String name, String description) {
        return new Project(id, name, description, createdByUserId, createdAt);
    }

    public ProjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
