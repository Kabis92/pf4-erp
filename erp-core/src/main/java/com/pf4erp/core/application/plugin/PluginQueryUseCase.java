package com.pf4erp.core.application.plugin;

import com.pf4erp.core.application.port.out.PluginRegistryPort;
import com.pf4erp.core.domain.plugin.PluginDescriptor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class PluginQueryUseCase {

    private final PluginRegistryPort pluginRegistryPort;

    public PluginQueryUseCase(PluginRegistryPort pluginRegistryPort) {
        this.pluginRegistryPort = pluginRegistryPort;
    }

    public List<PluginDescriptor> listStartedPlugins() {
        return pluginRegistryPort.listStartedPlugins();
    }

    public List<PluginDescriptor> listAllPlugins() {
        return pluginRegistryPort.listAllPlugins();
    }

    public Optional<PluginDescriptor> getPlugin(String pluginId) {
        return pluginRegistryPort.findById(pluginId);
    }

    public Optional<InputStream> openAsset(String pluginId, String relativePath) {
        return pluginRegistryPort.openAsset(pluginId, relativePath);
    }

    public PluginDescriptor start(String pluginId) {
        return pluginRegistryPort.startPlugin(pluginId);
    }

    public PluginDescriptor stop(String pluginId) {
        return pluginRegistryPort.stopPlugin(pluginId);
    }

    public void delete(String pluginId) {
        pluginRegistryPort.deletePlugin(pluginId);
    }

    public PluginDescriptor install(InputStream jarContent, String originalFilename) {
        return pluginRegistryPort.installPlugin(jarContent, originalFilename);
    }
}
