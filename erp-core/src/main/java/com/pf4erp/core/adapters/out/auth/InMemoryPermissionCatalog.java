package com.pf4erp.core.adapters.out.auth;

import com.pf4erp.core.application.port.out.PermissionCatalogPort;
import com.pf4erp.core.domain.auth.CorePermissions;
import com.pf4erp.core.domain.auth.PermissionDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPermissionCatalog implements PermissionCatalogPort {

    private final ConcurrentHashMap<String, PermissionDefinition> byCode = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        ensureCorePermissions();
    }

    @Override
    public void ensureCorePermissions() {
        for (PermissionDefinition permission : CorePermissions.all()) {
            byCode.put(permission.getCode(), permission);
        }
    }

    @Override
    public void registerPluginPermissions(String pluginId, Collection<PermissionDefinition> permissions) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        // Deactivate previous entries for this plugin that are no longer declared
        for (PermissionDefinition existing : new ArrayList<>(byCode.values())) {
            if (pluginId.equals(existing.getPluginId())) {
                byCode.put(existing.getCode(), existing.withActive(false));
            }
        }
        if (permissions == null) {
            return;
        }
        for (PermissionDefinition permission : permissions) {
            if (permission == null || permission.getCode() == null || permission.getCode().isBlank()) {
                continue;
            }
            byCode.put(permission.getCode(), new PermissionDefinition(
                    permission.getCode(),
                    permission.getLabel(),
                    pluginId,
                    true
            ));
        }
    }

    @Override
    public void deactivatePlugin(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            return;
        }
        for (PermissionDefinition existing : new ArrayList<>(byCode.values())) {
            if (pluginId.equals(existing.getPluginId())) {
                byCode.put(existing.getCode(), existing.withActive(false));
            }
        }
    }

    @Override
    public List<PermissionDefinition> listAll() {
        return byCode.values().stream()
                .sorted(Comparator
                        .comparing((PermissionDefinition p) -> p.isCore() ? 0 : 1)
                        .thenComparing(p -> p.getPluginId() != null ? p.getPluginId() : "")
                        .thenComparing(PermissionDefinition::getCode))
                .toList();
    }

    @Override
    public List<PermissionDefinition> listActive() {
        return listAll().stream().filter(PermissionDefinition::isActive).toList();
    }

    @Override
    public Optional<PermissionDefinition> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }
}
