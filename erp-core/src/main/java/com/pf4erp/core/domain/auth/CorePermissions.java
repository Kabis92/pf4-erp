package com.pf4erp.core.domain.auth;

import java.util.List;

/**
 * Permissions fixes du Core (toujours présentes dans le catalogue).
 */
public final class CorePermissions {

    public static final String USER_READ = "core.user.read";
    public static final String USER_WRITE = "core.user.write";
    public static final String ROLE_READ = "core.role.read";
    public static final String ROLE_WRITE = "core.role.write";
    public static final String PLUGIN_ADMIN = "core.plugin.admin";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private CorePermissions() {
    }

    public static List<PermissionDefinition> all() {
        return List.of(
                new PermissionDefinition(USER_READ, "Consulter les utilisateurs", null, true),
                new PermissionDefinition(USER_WRITE, "Créer / modifier / supprimer les utilisateurs", null, true),
                new PermissionDefinition(ROLE_READ, "Consulter les rôles", null, true),
                new PermissionDefinition(ROLE_WRITE, "Créer / modifier / supprimer les rôles", null, true),
                new PermissionDefinition(PLUGIN_ADMIN, "Administrer les plugins", null, true)
        );
    }
}
