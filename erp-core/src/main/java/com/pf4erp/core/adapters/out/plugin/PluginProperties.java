package com.pf4erp.core.adapters.out.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "pf4erp.plugin")
public class PluginProperties {

    /**
     * deployment | development
     */
    private String runtimeMode = "deployment";
    private String pluginsDir = "./plugins";
    private List<String> fixedPluginPath = new ArrayList<>();

    public String getRuntimeMode() {
        return runtimeMode;
    }

    public void setRuntimeMode(String runtimeMode) {
        this.runtimeMode = runtimeMode;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public List<String> getFixedPluginPath() {
        return fixedPluginPath;
    }

    public void setFixedPluginPath(List<String> fixedPluginPath) {
        this.fixedPluginPath = fixedPluginPath;
    }

    public boolean isDevelopment() {
        return "development".equalsIgnoreCase(runtimeMode);
    }
}
