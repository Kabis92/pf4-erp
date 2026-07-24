package com.pf4erp.core.domain.plugin;

import java.util.List;
import java.util.Objects;

public final class PluginDescriptor {

    private final String id;
    private final String version;
    private final String displayName;
    private final String description;
    private final String requires;
    private final String uiEntrypoint;
    private final String uiStyle;
    private final List<String> uiSlots;
    private final boolean started;
    private final boolean deletable;
    private final boolean apiDocsEnabled;
    private final String apiDocsGroup;
    private final String apiDocsUrl;

    public PluginDescriptor(
            String id,
            String version,
            String displayName,
            String description,
            String requires,
            String uiEntrypoint,
            String uiStyle,
            List<String> uiSlots,
            boolean started,
            boolean deletable,
            boolean apiDocsEnabled,
            String apiDocsGroup,
            String apiDocsUrl
    ) {
        this.id = Objects.requireNonNull(id);
        this.version = Objects.requireNonNull(version);
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : "";
        this.requires = requires != null ? requires : "*";
        this.uiEntrypoint = uiEntrypoint != null ? uiEntrypoint : "ui/main.js";
        this.uiStyle = uiStyle != null ? uiStyle : "ui/style.css";
        this.uiSlots = uiSlots != null ? List.copyOf(uiSlots) : List.of();
        this.started = started;
        this.deletable = deletable;
        this.apiDocsEnabled = apiDocsEnabled;
        this.apiDocsGroup = apiDocsGroup;
        this.apiDocsUrl = apiDocsUrl;
    }

    public PluginDescriptor withStarted(boolean started) {
        return new PluginDescriptor(
                id, version, displayName, description, requires,
                uiEntrypoint, uiStyle, uiSlots, started, deletable,
                apiDocsEnabled, apiDocsGroup, apiDocsUrl
        );
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getRequires() {
        return requires;
    }

    public String getUiEntrypoint() {
        return uiEntrypoint;
    }

    public String getUiStyle() {
        return uiStyle;
    }

    public List<String> getUiSlots() {
        return uiSlots;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public boolean isApiDocsEnabled() {
        return apiDocsEnabled;
    }

    public String getApiDocsGroup() {
        return apiDocsGroup;
    }

    public String getApiDocsUrl() {
        return apiDocsUrl;
    }
}
