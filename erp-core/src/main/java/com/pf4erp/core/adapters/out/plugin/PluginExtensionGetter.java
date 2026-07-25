package com.pf4erp.core.adapters.out.plugin;

import com.pf4erp.api.ExtensionGetter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Résout les beans d’extension exposés par les contextes Spring des plugins démarrés.
 */
@Component
public class PluginExtensionGetter implements ExtensionGetter {

    private final Supplier<Iterable<ConfigurableApplicationContext>> pluginContexts;

    public PluginExtensionGetter(PluginContextRegistry contextRegistry) {
        this.pluginContexts = contextRegistry::activeContexts;
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type) {
        Objects.requireNonNull(type, "type");
        List<T> result = new ArrayList<>();
        for (ConfigurableApplicationContext ctx : pluginContexts.get()) {
            if (!ctx.isActive()) {
                continue;
            }
            result.addAll(ctx.getBeansOfType(type).values());
        }
        return List.copyOf(result);
    }
}
