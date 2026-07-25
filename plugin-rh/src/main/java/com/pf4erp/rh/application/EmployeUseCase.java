package com.pf4erp.rh.application;

import com.pf4erp.rh.api.event.EmployeDeletedEvent;
import com.pf4erp.rh.api.event.EmployeLinkedToUserEvent;
import com.pf4erp.rh.api.event.EmployeUnlinkedFromUserEvent;
import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmployeUseCase {

    private final EmployeRepository employeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeUseCase(EmployeRepository employeRepository, ApplicationEventPublisher eventPublisher) {
        this.employeRepository = Objects.requireNonNull(employeRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
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
        Employe saved = employeRepository.save(employe);
        publishLinkChanges(null, saved);
        return saved;
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
        Employe updated = employeRepository.save(existing.with(nom, email, poste, dateEmbauche, userId));
        publishLinkChanges(existing, updated);
        return updated;
    }

    public void delete(EmployeId id) {
        Employe existing = get(id);
        employeRepository.delete(id);
        eventPublisher.publishEvent(new EmployeDeletedEvent(this, existing.getId().getValue(), existing.getUserId()));
    }

    public long count() {
        return employeRepository.count();
    }

    private void publishLinkChanges(Employe before, Employe after) {
        String previousUserId = before != null ? before.getUserId() : null;
        String nextUserId = after.getUserId();
        if (Objects.equals(previousUserId, nextUserId)) {
            return;
        }
        if (previousUserId != null) {
            eventPublisher.publishEvent(new EmployeUnlinkedFromUserEvent(
                    this,
                    after.getId().getValue(),
                    previousUserId
            ));
        }
        if (nextUserId != null) {
            eventPublisher.publishEvent(new EmployeLinkedToUserEvent(
                    this,
                    after.getId().getValue(),
                    nextUserId,
                    after.getNom()
            ));
        }
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
