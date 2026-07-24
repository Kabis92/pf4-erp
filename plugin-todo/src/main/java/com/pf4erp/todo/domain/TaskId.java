package com.pf4erp.todo.domain;

import java.util.Objects;
import java.util.UUID;

public final class TaskId {
    private final String value;

    private TaskId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskId is required");
        }
        this.value = value;
    }

    public static TaskId of(String value) {
        return new TaskId(value);
    }

    public static TaskId generate() {
        return new TaskId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskId that)) return false;
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
