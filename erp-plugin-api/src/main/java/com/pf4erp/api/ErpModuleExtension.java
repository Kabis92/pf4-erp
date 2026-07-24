package com.pf4erp.api;

import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Set;

/**
 * Extension point Java exposé par chaque plugin métier.
 */
public interface ErpModuleExtension extends ExtensionPoint {

    String getModuleId();

    String getDisplayName();

    /** Permissions déclarées par le module (ex. rh.employe.read). */
    Set<String> getPermissions();

    /** Noms de slots UI auxquels le module contribue (ex. dashboard.widgets). */
    List<String> getUiSlots();

    /**
     * Classes Spring {@code @Configuration} à enregistrer dans le contexte enfant du plugin.
     * Peut retourner un tableau vide si le plugin n'a pas de beans Spring.
     */
    Class<?>[] getSpringConfigurations();
}
