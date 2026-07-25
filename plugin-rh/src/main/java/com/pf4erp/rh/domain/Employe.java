package com.pf4erp.rh.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class Employe {

    private final EmployeId id;
    private final String nom;
    private final String email;
    private final String telephone;
    private final String poste;
    private final LocalDate dateEmbauche;
    private final String userId;
    private final EmployeStatut statut;

    public Employe(
            EmployeId id,
            String nom,
            String email,
            String telephone,
            String poste,
            LocalDate dateEmbauche,
            String userId,
            EmployeStatut statut
    ) {
        this.id = Objects.requireNonNull(id);
        this.nom = requireText(nom, "nom");
        this.email = requireText(email, "email");
        this.telephone = blankToNull(telephone);
        this.poste = requireText(poste, "poste");
        this.dateEmbauche = Objects.requireNonNull(dateEmbauche, "dateEmbauche");
        this.userId = blankToNull(userId);
        this.statut = statut != null ? statut : EmployeStatut.ACTIF;
    }

    public Employe with(
            String nom,
            String email,
            String telephone,
            String poste,
            LocalDate dateEmbauche,
            String userId,
            EmployeStatut statut
    ) {
        return new Employe(this.id, nom, email, telephone, poste, dateEmbauche, userId, statut);
    }

    public Employe withUserId(String userId) {
        return new Employe(id, nom, email, telephone, poste, dateEmbauche, userId, statut);
    }

    public EmployeId getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getPoste() {
        return poste;
    }

    public LocalDate getDateEmbauche() {
        return dateEmbauche;
    }

    public String getUserId() {
        return userId;
    }

    public EmployeStatut getStatut() {
        return statut;
    }

    public boolean hasAccess() {
        return userId != null;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
