package com.pf4erp.core.adapters.out.plugin;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre des contextes Spring enfants des plugins (pour ExtensionGetter / diagnostics).
 */
@Component
public class PluginContextRegistry {

    private final Map<String, ConfigurableApplicationContext> contexts = new ConcurrentHashMap<>();

    public void register(String pluginId, ConfigurableApplicationContext context) {
        contexts.put(pluginId, context);
    }

    public void unregister(String pluginId) {
        contexts.remove(pluginId);
    }

    public Collection<ConfigurableApplicationContext> activeContexts() {
        return contexts.values().stream().filter(ConfigurableApplicationContext::isActive).toList();
    }
}
