package com.pf4erp.rh.application;

import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmployeUseCase {

    private final EmployeRepository employeRepository;

    public EmployeUseCase(EmployeRepository employeRepository) {
        this.employeRepository = Objects.requireNonNull(employeRepository);
    }

    public List<Employe> list() {
        return employeRepository.findAll();
    }

    public Employe get(EmployeId id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new EmployeNotFoundException(id));
    }

    public Optional<Employe> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return employeRepository.findByUserId(userId);
    }

    public Employe create(String nom, String email, String poste, LocalDate dateEmbauche, String userId) {
        validateUserLink(userId, null);
        Employe employe = new Employe(EmployeId.generate(), nom, email, poste, dateEmbauche, userId);
        return employeRepository.save(employe);
    }

    public Employe update(
            EmployeId id,
            String nom,
            String email,
            String poste,
            LocalDate dateEmbauche,
            String userId
    ) {
        Employe existing = get(id);
        validateUserLink(userId, id);
        return employeRepository.save(existing.with(nom, email, poste, dateEmbauche, userId));
    }

    public void delete(EmployeId id) {
        get(id);
        employeRepository.delete(id);
    }

    public long count() {
        return employeRepository.count();
    }

    private void validateUserLink(String userId, EmployeId excludingId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        if (employeRepository.existsByUserId(userId.trim(), excludingId)) {
            throw new UserAlreadyLinkedException(userId.trim());
        }
    }

    public static class EmployeNotFoundException extends RuntimeException {
        public EmployeNotFoundException(EmployeId id) {
            super("Employé introuvable: " + id);
        }
    }

    public static class UserAlreadyLinkedException extends RuntimeException {
        public UserAlreadyLinkedException(String userId) {
            super("Ce compte utilisateur est déjà lié à un autre employé: " + userId);
        }
    }
}
