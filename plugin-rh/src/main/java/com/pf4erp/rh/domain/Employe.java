package com.pf4erp.rh.domain;

import java.time.LocalDate;
import java.util.Objects;

public final class Employe {

    private final EmployeId id;
    private final String nom;
    private final String email;
    private final String poste;
    private final LocalDate dateEmbauche;

    public Employe(EmployeId id, String nom, String email, String poste, LocalDate dateEmbauche) {
        this.id = Objects.requireNonNull(id);
        this.nom = requireText(nom, "nom");
        this.email = requireText(email, "email");
        this.poste = requireText(poste, "poste");
        this.dateEmbauche = Objects.requireNonNull(dateEmbauche, "dateEmbauche");
    }

    public Employe with(String nom, String email, String poste, LocalDate dateEmbauche) {
        return new Employe(this.id, nom, email, poste, dateEmbauche);
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

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
