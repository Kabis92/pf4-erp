package com.pf4erp.todo.domain;

import java.util.Objects;
import java.util.UUID;

public final class ProjectId {
    private final String value;

    private ProjectId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProjectId is required");
        }
        this.value = value;
    }

    public static ProjectId of(String value) {
        return new ProjectId(value);
    }

    public static ProjectId generate() {
        return new ProjectId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
