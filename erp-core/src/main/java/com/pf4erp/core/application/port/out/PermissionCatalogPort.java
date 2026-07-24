package com.pf4erp.core.application.port.out;

import com.pf4erp.core.domain.auth.PermissionDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue runtime des permissions (core + plugins démarrés / connus).
 */
public interface PermissionCatalogPort {

    void ensureCorePermissions();

    void registerPluginPermissions(String pluginId, Collection<PermissionDefinition> permissions);

    void deactivatePlugin(String pluginId);

    List<PermissionDefinition> listAll();

    List<PermissionDefinition> listActive();

    Optional<PermissionDefinition> findByCode(String code);
}
