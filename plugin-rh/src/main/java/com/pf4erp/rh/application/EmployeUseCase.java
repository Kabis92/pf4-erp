package com.pf4erp.rh.application;

import com.pf4erp.rh.api.event.EmployeDeletedEvent;
import com.pf4erp.rh.api.event.EmployeLinkedToUserEvent;
import com.pf4erp.rh.api.event.EmployeUnlinkedFromUserEvent;
import com.pf4erp.rh.application.port.EmployeRepository;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;
import com.pf4erp.rh.domain.EmployeStatut;
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

    public Employe create(
            String nom,
            String email,
            String telephone,
            String poste,
            LocalDate dateEmbauche,
            String userId,
            EmployeStatut statut
    ) {
        validateUserLink(userId, null);
        EmployeStatut resolved = statut != null ? statut : EmployeStatut.ACTIF;
        Employe employe = new Employe(
                EmployeId.generate(),
                nom,
                email,
                telephone,
                poste,
                dateEmbauche,
                userId,
                resolved
        );
        Employe saved = employeRepository.save(employe);
        publishLinkChanges(null, saved);
        return saved;
    }

    public Employe update(
            EmployeId id,
            String nom,
            String email,
            String telephone,
            String poste,
            LocalDate dateEmbauche,
            String userId,
            EmployeStatut statut
    ) {
        Employe existing = get(id);
        validateUserLink(userId, id);
        EmployeStatut resolved = statut != null ? statut : existing.getStatut();
        Employe updated = employeRepository.save(
                existing.with(nom, email, telephone, poste, dateEmbauche, userId, resolved)
        );
        publishLinkChanges(existing, updated);
        return updated;
    }

    /** Active / désactive l’accès système (lien compte). */
    public Employe setSystemAccess(EmployeId id, boolean enabled, String userIdWhenEnabling) {
        Employe existing = get(id);
        if (!enabled) {
            Employe updated = employeRepository.save(existing.withUserId(null));
            publishLinkChanges(existing, updated);
            return updated;
        }
        String targetUserId = userIdWhenEnabling != null && !userIdWhenEnabling.isBlank()
                ? userIdWhenEnabling.trim()
                : existing.getUserId();
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new AccessUserRequiredException();
        }
        validateUserLink(targetUserId, id);
        Employe updated = employeRepository.save(existing.withUserId(targetUserId));
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

    public static class AccessUserRequiredException extends RuntimeException {
        public AccessUserRequiredException() {
            super("Sélectionnez un compte utilisateur pour activer l’accès système");
        }
    }
}
