package com.pf4erp.rh.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class Employe {

    private final EmployeId id;
    private final String nom;
    private final String email;
    private final String poste;
    private final LocalDate dateEmbauche;
    private final String userId;

    public Employe(
            EmployeId id,
            String nom,
            String email,
            String poste,
            LocalDate dateEmbauche,
            String userId
    ) {
        this.id = Objects.requireNonNull(id);
        this.nom = requireText(nom, "nom");
        this.email = requireText(email, "email");
        this.poste = requireText(poste, "poste");
        this.dateEmbauche = Objects.requireNonNull(dateEmbauche, "dateEmbauche");
        this.userId = blankToNull(userId);
    }

    public Employe with(String nom, String email, String poste, LocalDate dateEmbauche, String userId) {
        return new Employe(this.id, nom, email, poste, dateEmbauche, userId);
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

    public String getPoste() {
        return poste;
    }

    public LocalDate getDateEmbauche() {
        return dateEmbauche;
    }

    public String getUserId() {
        return userId;
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
