package com.pf4erp.todo.domain;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    public static TaskStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TODO;
        }
        return TaskStatus.valueOf(raw.trim().toUpperCase());
    }
}
