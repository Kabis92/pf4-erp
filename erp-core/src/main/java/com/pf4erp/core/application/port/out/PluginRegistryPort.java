package com.pf4erp.core.application.port.out;

import com.pf4erp.core.domain.plugin.PluginDescriptor;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface PluginRegistryPort {

    /** Plugins démarrés (shell UI). */
    List<PluginDescriptor> listStartedPlugins();

    /** Tous les plugins gérés (admin). */
    List<PluginDescriptor> listAllPlugins();

    Optional<PluginDescriptor> findById(String pluginId);

    Optional<InputStream> openAsset(String pluginId, String relativePath);

    PluginDescriptor startPlugin(String pluginId);

    PluginDescriptor stopPlugin(String pluginId);

    void deletePlugin(String pluginId);

    /** Installe un JAR dans plugins-dir, le charge et le démarre. */
    PluginDescriptor installPlugin(InputStream jarContent, String originalFilename);
}
