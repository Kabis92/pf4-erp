package com.pf4erp.api;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux extensions / beans fournis par les plugins démarrés.
 *
 * <p>Équivalent Halo {@code ExtensionGetter} :
 * <a href="https://docs.halo.run/developer-guide/plugin/interaction/making-plugin-extensible">plugin extensible</a>.
 *
 * <p>Injecté depuis le contexte parent (Core) dans chaque contexte enfant de plugin.
 * Ne pas dépendre d’un autre plugin JAR : dépendre uniquement de l’interface
 * (ex. {@code plugin-rh-api}) + de ce getter.
 */
public interface ExtensionGetter {

    /**
     * Toutes les implémentations actives du type demandé (beans Spring des contextes plugins).
     */
    <T> List<T> getExtensions(Class<T> type);

    /**
     * Première implémentation disponible (cas SINGLE_INSTANCE).
     */
    default <T> Optional<T> getExtension(Class<T> type) {
        List<T> all = getExtensions(type);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getFirst());
    }
}
