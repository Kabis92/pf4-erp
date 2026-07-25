package com.pf4erp.core.adapters.out.plugin;

import com.pf4erp.api.SharedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bus d’événements inter-plugins (inspiration Halo {@code @SharedEvent}).
 *
 * <p>Chaque plugin a un contexte Spring enfant isolé : un {@code publishEvent} local
 * n’atteint pas les autres plugins. Ce hub relaie les événements annotés
 * {@link SharedEvent} vers le Core puis vers les autres contextes plugins.
 */
@Component
public class SharedEventHub {

    private static final Logger log = LoggerFactory.getLogger(SharedEventHub.class);

    private final ApplicationContext parentContext;
    private final Map<String, ConfigurableApplicationContext> pluginContexts = new ConcurrentHashMap<>();
    private final AtomicBoolean dispatching = new AtomicBoolean(false);

    public SharedEventHub(ApplicationContext parentContext) {
        this.parentContext = parentContext;
    }

    public void registerPluginContext(String pluginId, ConfigurableApplicationContext child) {
        pluginContexts.put(pluginId, child);
        child.addApplicationListener(event -> {
            ApplicationEvent applicationEvent = unwrap(event);
            if (applicationEvent == null || !isShared(applicationEvent)) {
                return;
            }
            if (dispatching.get()) {
                return;
            }
            dispatch(pluginId, applicationEvent);
        });
        log.debug("SharedEventHub listening on plugin context {}", pluginId);
    }

    public void unregisterPluginContext(String pluginId) {
        pluginContexts.remove(pluginId);
    }

    /**
     * Publication depuis le Core (parent) vers tous les plugins.
     */
    public void publishFromCore(ApplicationEvent event) {
        if (!isShared(event)) {
            parentContext.publishEvent(event);
            return;
        }
        dispatch(null, event);
    }

    private void dispatch(String sourcePluginId, ApplicationEvent event) {
        if (!dispatching.compareAndSet(false, true)) {
            return;
        }
        try {
            log.debug(
                    "Bridging @SharedEvent {} from {}",
                    event.getClass().getSimpleName(),
                    sourcePluginId != null ? "plugin:" + sourcePluginId : "core"
            );
            parentContext.publishEvent(event);
            for (Map.Entry<String, ConfigurableApplicationContext> entry : pluginContexts.entrySet()) {
                if (entry.getKey().equals(sourcePluginId)) {
                    continue;
                }
                ConfigurableApplicationContext target = entry.getValue();
                if (target.isActive()) {
                    target.publishEvent(event);
                }
            }
        } finally {
            dispatching.set(false);
        }
    }

    private static boolean isShared(ApplicationEvent event) {
        return event.getClass().isAnnotationPresent(SharedEvent.class);
    }

    private static ApplicationEvent unwrap(Object event) {
        if (event instanceof ApplicationEvent applicationEvent) {
            return applicationEvent;
        }
        if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof ApplicationEvent nested) {
            return nested;
        }
        return null;
    }
}
