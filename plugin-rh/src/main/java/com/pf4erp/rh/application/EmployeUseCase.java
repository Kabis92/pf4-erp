package com.pf4erp.rh.application;

import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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

    public Employe create(String nom, String email, String poste, LocalDate dateEmbauche) {
        Employe employe = new Employe(EmployeId.generate(), nom, email, poste, dateEmbauche);
        return employeRepository.save(employe);
    }

    public Employe update(EmployeId id, String nom, String email, String poste, LocalDate dateEmbauche) {
        Employe existing = get(id);
        return employeRepository.save(existing.with(nom, email, poste, dateEmbauche));
    }

    public void delete(EmployeId id) {
        get(id);
        employeRepository.delete(id);
    }

    public long count() {
        return employeRepository.count();
    }

    public static class EmployeNotFoundException extends RuntimeException {
        public EmployeNotFoundException(EmployeId id) {
            super("Employé introuvable: " + id);
        }
    }
}
