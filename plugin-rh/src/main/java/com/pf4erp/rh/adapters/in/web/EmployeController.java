package com.pf4erp.rh.adapters.in.web;

import com.pf4erp.rh.application.EmployeUseCase;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;
import com.pf4erp.rh.domain.EmployeStatut;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rh/employes")
@Tag(name = "RH - Employés")
public class EmployeController {

    private final EmployeUseCase employeUseCase;

    public EmployeController(EmployeUseCase employeUseCase) {
        this.employeUseCase = employeUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('rh.employe.read')")
    public List<EmployeDto> list() {
        return employeUseCase.list().stream().map(this::toDto).toList();
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('rh.employe.read')")
    public CountDto count() {
        return new CountDto(employeUseCase.count());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public EmployeDto me() {
        String userId = currentUserId();
        return employeUseCase.findByUserId(userId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun employé lié à votre compte"
                ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('rh.employe.read')")
    public EmployeDto get(@PathVariable String id) {
        try {
            return toDto(employeUseCase.get(EmployeId.of(id)));
        } catch (EmployeUseCase.EmployeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('rh.employe.write')")
    public EmployeDto create(@Valid @RequestBody EmployeRequest request) {
        try {
            return toDto(employeUseCase.create(
                    request.nom(),
                    request.email(),
                    request.telephone(),
                    request.poste(),
                    request.dateEmbauche(),
                    request.userId(),
                    parseStatut(request.statut())
            ));
        } catch (EmployeUseCase.UserAlreadyLinkedException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh.employe.write')")
    public EmployeDto update(@PathVariable String id, @Valid @RequestBody EmployeRequest request) {
        try {
            return toDto(employeUseCase.update(
                    EmployeId.of(id),
                    request.nom(),
                    request.email(),
                    request.telephone(),
                    request.poste(),
                    request.dateEmbauche(),
                    request.userId(),
                    parseStatut(request.statut())
            ));
        } catch (EmployeUseCase.EmployeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (EmployeUseCase.UserAlreadyLinkedException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }
    }

    @PutMapping("/{id}/access")
    @PreAuthorize("hasAuthority('rh.employe.write')")
    public EmployeDto setAccess(@PathVariable String id, @Valid @RequestBody AccessRequest request) {
        try {
            return toDto(employeUseCase.setSystemAccess(
                    EmployeId.of(id),
                    request.enabled(),
                    request.userId()
            ));
        } catch (EmployeUseCase.EmployeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (EmployeUseCase.UserAlreadyLinkedException | EmployeUseCase.AccessUserRequiredException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('rh.employe.write')")
    public void delete(@PathVariable String id) {
        try {
            employeUseCase.delete(EmployeId.of(id));
        } catch (EmployeUseCase.EmployeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private EmployeStatut parseStatut(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return EmployeStatut.from(raw);
    }

    @SuppressWarnings("unchecked")
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide");
        }
        if (authentication.getDetails() instanceof Map<?, ?> map) {
            Object userId = map.get("userId");
            if (userId != null && !userId.toString().isBlank()) {
                return userId.toString();
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "userId absent du jeton");
    }

    private EmployeDto toDto(Employe employe) {
        return new EmployeDto(
                employe.getId().getValue(),
                employe.getNom(),
                employe.getEmail(),
                employe.getTelephone(),
                employe.getPoste(),
                employe.getDateEmbauche(),
                employe.getUserId(),
                employe.getStatut().name(),
                employe.hasAccess()
        );
    }

    public record EmployeRequest(
            @NotBlank String nom,
            @NotBlank @Email String email,
            String telephone,
            @NotBlank String poste,
            @NotNull LocalDate dateEmbauche,
            String userId,
            String statut
    ) {
    }

    public record AccessRequest(
            boolean enabled,
            String userId
    ) {
    }

    public record EmployeDto(
            String id,
            String nom,
            String email,
            String telephone,
            String poste,
            LocalDate dateEmbauche,
            String userId,
            String statut,
            boolean hasAccess
    ) {
    }

    public record CountDto(long count) {
    }
}
