package com.pf4erp.rh.api;

/**
 * Vue stable d’un employé RH destinée aux autres modules.
 * Ne contient que des champs utiles hors du domaine RH interne.
 */
public record EmployeRef(
        String id,
        String nom,
        String email,
        String poste,
        String userId
) {
}
