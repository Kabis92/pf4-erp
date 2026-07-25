package com.pf4erp.rh.domain;

/**
 * Statuts cycle de vie (inspiré UI Employees : Active / On Leave / Inactive).
 */
public enum EmployeStatut {
    ACTIF,
    CONGE,
    INACTIF;

    public static EmployeStatut from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIF;
        }
        String value = raw.trim().toUpperCase();
        // Compat données V1 (ONBOARDING / SORTIE)
        return switch (value) {
            case "ONBOARDING" -> INACTIF;
            case "SORTIE" -> INACTIF;
            default -> EmployeStatut.valueOf(value);
        };
    }
}
