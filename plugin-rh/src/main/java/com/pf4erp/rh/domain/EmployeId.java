package com.pf4erp.rh.domain;

import java.util.Objects;
import java.util.UUID;

public final class EmployeId {

    private final String value;

    private EmployeId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EmployeId is required");
        }
        this.value = value;
    }

    public static EmployeId of(String value) {
        return new EmployeId(value);
    }

    public static EmployeId generate() {
        return new EmployeId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmployeId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
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
