package com.pf4erp.core.adapters.in.web.auth;

import com.pf4erp.core.application.auth.RoleAdminUseCase;
import com.pf4erp.core.domain.auth.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/roles")
@Tag(name = "Admin - Rôles")
public class RoleAdminController {

    private final RoleAdminUseCase roleAdminUseCase;

    public RoleAdminController(RoleAdminUseCase roleAdminUseCase) {
        this.roleAdminUseCase = roleAdminUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('core.role.read')")
    @Operation(summary = "Lister les rôles")
    public List<RoleDto> list() {
        return roleAdminUseCase.list().stream().map(RoleDto::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('core.role.read')")
    @Operation(summary = "Détail d’un rôle")
    public RoleDto get(@PathVariable String id) {
        return RoleDto.from(roleAdminUseCase.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('core.role.write')")
    @Operation(summary = "Créer un rôle")
    public RoleDto create(@Valid @RequestBody CreateRoleRequest request) {
        return RoleDto.from(roleAdminUseCase.create(
                request.code(),
                request.label(),
                request.description(),
                request.permissionCodes() != null ? request.permissionCodes() : Set.of()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('core.role.write')")
    @Operation(summary = "Mettre à jour un rôle")
    public RoleDto update(@PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
        return RoleDto.from(roleAdminUseCase.update(
                id,
                request.label(),
                request.description(),
                request.permissionCodes()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('core.role.write')")
    @Operation(summary = "Supprimer un rôle")
    public void delete(@PathVariable String id) {
        roleAdminUseCase.delete(id);
    }

    public record CreateRoleRequest(
            @NotBlank String code,
            String label,
            String description,
            Set<String> permissionCodes
    ) {
    }

    public record UpdateRoleRequest(
            String label,
            String description,
            Set<String> permissionCodes
    ) {
    }

    public record RoleDto(
            String id,
            String code,
            String label,
            String description,
            Set<String> permissionCodes,
            boolean system
    ) {
        static RoleDto from(Role role) {
            return new RoleDto(
                    role.getId(),
                    role.getCode(),
                    role.getLabel(),
                    role.getDescription(),
                    role.getPermissionCodes(),
                    role.isSystem()
            );
        }
    }
}
