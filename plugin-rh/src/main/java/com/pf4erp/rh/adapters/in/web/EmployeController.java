package com.pf4erp.rh.adapters.in.web;

import com.pf4erp.rh.application.EmployeUseCase;
import com.pf4erp.rh.domain.Employe;
import com.pf4erp.rh.domain.EmployeId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return toDto(employeUseCase.create(
                request.nom(),
                request.email(),
                request.poste(),
                request.dateEmbauche()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rh.employe.write')")
    public EmployeDto update(@PathVariable String id, @Valid @RequestBody EmployeRequest request) {
        try {
            return toDto(employeUseCase.update(
                    EmployeId.of(id),
                    request.nom(),
                    request.email(),
                    request.poste(),
                    request.dateEmbauche()
            ));
        } catch (EmployeUseCase.EmployeNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
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

    private EmployeDto toDto(Employe employe) {
        return new EmployeDto(
                employe.getId().getValue(),
                employe.getNom(),
                employe.getEmail(),
                employe.getPoste(),
                employe.getDateEmbauche()
        );
    }

    public record EmployeRequest(
            @NotBlank String nom,
            @NotBlank @Email String email,
            @NotBlank String poste,
            @NotNull LocalDate dateEmbauche
    ) {
    }

    public record EmployeDto(
            String id,
            String nom,
            String email,
            String poste,
            LocalDate dateEmbauche
    ) {
    }

    public record CountDto(long count) {
    }
}
