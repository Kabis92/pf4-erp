package com.pf4erp.core.domain.auth;

import java.util.Objects;
import java.util.Set;

/**
 * Rôle applicatif : ensemble de codes permission (core ou plugin).
 */
public final class Role {

    private final String id;
    private final String code;
    private final String label;
    private final String description;
    private final Set<String> permissionCodes;
    private final boolean system;

    public Role(
            String id,
            String code,
            String label,
            String description,
            Set<String> permissionCodes,
            boolean system
    ) {
        this.id = Objects.requireNonNull(id);
        this.code = Objects.requireNonNull(code);
        this.label = Objects.requireNonNull(label);
        this.description = description != null ? description : "";
        this.permissionCodes = Set.copyOf(permissionCodes);
        this.system = system;
    }

    public Role withLabel(String label) {
        return new Role(id, code, label, description, permissionCodes, system);
    }

    public Role withDescription(String description) {
        return new Role(id, code, label, description, permissionCodes, system);
    }

    public Role withPermissionCodes(Set<String> permissionCodes) {
        return new Role(id, code, label, description, permissionCodes, system);
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getPermissionCodes() {
        return permissionCodes;
    }

    public boolean isSystem() {
        return system;
    }
}
