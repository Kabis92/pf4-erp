package com.pf4erp.core.adapters.in.web.auth;

import com.pf4erp.core.application.auth.PermissionQueryUseCase;
import com.pf4erp.core.domain.auth.PermissionDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
@Tag(name = "Admin - Permissions")
public class PermissionAdminController {

    private final PermissionQueryUseCase permissionQueryUseCase;

    public PermissionAdminController(PermissionQueryUseCase permissionQueryUseCase) {
        this.permissionQueryUseCase = permissionQueryUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('core.role.read') or hasAuthority('core.user.read')")
    @Operation(summary = "Catalogue des permissions (core + plugins)")
    public List<PermissionDto> list() {
        return permissionQueryUseCase.listAll().stream().map(PermissionDto::from).toList();
    }

    public record PermissionDto(
            String code,
            String label,
            String pluginId,
            boolean active,
            boolean core
    ) {
        static PermissionDto from(PermissionDefinition definition) {
            return new PermissionDto(
                    definition.getCode(),
                    definition.getLabel(),
                    definition.getPluginId(),
                    definition.isActive(),
                    definition.isCore()
            );
        }
    }
}
