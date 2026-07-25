package com.pf4erp.rh.adapters.out.api;

import com.pf4erp.rh.api.EmployeDirectory;
import com.pf4erp.rh.api.EmployeRef;
import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implémentation Spring de l’extension point {@link EmployeDirectory}.
 * Exposée comme bean pour être découverte via {@link com.pf4erp.api.ExtensionGetter}.
 */
public class SpringEmployeDirectory implements EmployeDirectory {

    private final EmployeRepository employeRepository;

    public SpringEmployeDirectory(EmployeRepository employeRepository) {
        this.employeRepository = Objects.requireNonNull(employeRepository);
    }

    @Override
    public Optional<EmployeRef> findById(String employeId) {
        if (employeId == null || employeId.isBlank()) {
            return Optional.empty();
        }
        return employeRepository.findById(EmployeId.of(employeId)).map(this::toRef);
    }

    @Override
    public Optional<EmployeRef> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return employeRepository.findByUserId(userId).map(this::toRef);
    }

    @Override
    public List<EmployeRef> listLinkedToUsers() {
        return employeRepository.findAll().stream()
                .filter(e -> e.getUserId() != null && !e.getUserId().isBlank())
                .map(this::toRef)
                .toList();
    }

    @Override
    public List<EmployeRef> listAll() {
        return employeRepository.findAll().stream().map(this::toRef).toList();
    }

    private EmployeRef toRef(Employe employe) {
        return new EmployeRef(
                employe.getId().getValue(),
                employe.getNom(),
                employe.getEmail(),
                employe.getPoste(),
                employe.getUserId()
        );
    }
}
