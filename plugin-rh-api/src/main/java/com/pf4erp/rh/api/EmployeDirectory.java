package com.pf4erp.rh.api;

import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Optional;

/**
 * Extension point RH : annuaire employés pour les autres plugins.
 *
 * <p>Pattern Halo — définir une interface {@link ExtensionPoint}, l’implémenter dans
 * le plugin fournisseur, la consommer via {@link com.pf4erp.api.ExtensionGetter}.
 *
 * <p>Type attendu : SINGLE_INSTANCE (un seul annuaire RH actif).
 */
public interface EmployeDirectory extends ExtensionPoint {

    Optional<EmployeRef> findById(String employeId);

    Optional<EmployeRef> findByUserId(String userId);

    /** Employés liés à un compte utilisateur ({@code userId} non null). */
    List<EmployeRef> listLinkedToUsers();

    List<EmployeRef> listAll();
}
