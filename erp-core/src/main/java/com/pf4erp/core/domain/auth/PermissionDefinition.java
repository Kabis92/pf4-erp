package com.pf4erp.core.domain.auth;

import java.util.Objects;

/**
 * Permission déclarée (core ou plugin). Le catalogue runtime n’est pas une collection Mongo.
 */
public final class PermissionDefinition {

    private final String code;
    private final String label;
    private final String pluginId;
    private final boolean active;

    public PermissionDefinition(String code, String label, String pluginId, boolean active) {
        this.code = Objects.requireNonNull(code);
        this.label = label != null && !label.isBlank() ? label : code;
        this.pluginId = pluginId;
        this.active = active;
    }

    public PermissionDefinition withActive(boolean active) {
        return new PermissionDefinition(code, label, pluginId, active);
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getPluginId() {
        return pluginId;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCore() {
        return pluginId == null || pluginId.isBlank();
    }
}
