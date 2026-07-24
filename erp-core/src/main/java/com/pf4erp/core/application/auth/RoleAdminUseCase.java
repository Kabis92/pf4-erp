package com.pf4erp.core.application.auth;

import com.pf4erp.core.application.port.out.PermissionCatalogPort;
import com.pf4erp.core.application.port.out.RoleRepository;
import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.CorePermissions;
import com.pf4erp.core.domain.auth.Role;
import com.pf4erp.core.domain.auth.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleAdminUseCase {

    private final RoleRepository roleRepository;
    private final PermissionCatalogPort permissionCatalog;
    private final UserAccountRepository userAccountRepository;

    public RoleAdminUseCase(
            RoleRepository roleRepository,
            PermissionCatalogPort permissionCatalog,
            UserAccountRepository userAccountRepository
    ) {
        this.roleRepository = roleRepository;
        this.permissionCatalog = permissionCatalog;
        this.userAccountRepository = userAccountRepository;
    }

    public List<Role> list() {
        return roleRepository.findAll();
    }

    public Role get(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle introuvable"));
    }

    public Role create(String code, String label, String description, Set<String> permissionCodes) {
        String normalized = normalizeCode(code);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code rôle requis");
        }
        if (roleRepository.existsByCode(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Code rôle déjà utilisé");
        }
        Role role = new Role(
                UUID.randomUUID().toString(),
                normalized,
                requireLabel(label, normalized),
                description != null ? description.trim() : "",
                validatePermissions(permissionCodes),
                false
        );
        return roleRepository.save(role);
    }

    public Role update(String id, String label, String description, Set<String> permissionCodes) {
        Role existing = get(id);
        String newLabel = label != null ? requireLabel(label, existing.getCode()) : existing.getLabel();
        String newDescription = description != null ? description.trim() : existing.getDescription();
        Set<String> perms = permissionCodes != null
                ? validatePermissions(permissionCodes)
                : existing.getPermissionCodes();

        // ADMIN system role: permissions are implicit (all active) — keep empty storage
        if (CorePermissions.ROLE_ADMIN.equals(existing.getCode())) {
            perms = Set.of();
        }

        Role updated = existing
                .withLabel(newLabel)
                .withDescription(newDescription)
                .withPermissionCodes(perms);
        return roleRepository.save(updated);
    }

    public void delete(String id) {
        Role existing = get(id);
        if (existing.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de supprimer un rôle système");
        }
        for (UserAccount user : userAccountRepository.findAll()) {
            if (user.getRoleCodes().contains(existing.getCode())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Rôle encore assigné à des utilisateurs (ex. " + user.getUsername() + ")");
            }
        }
        roleRepository.deleteById(id);
    }

    private Set<String> validatePermissions(Set<String> permissionCodes) {
        if (permissionCodes == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String code : permissionCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String trimmed = code.trim();
            if (permissionCatalog.findByCode(trimmed).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission inconnue: " + trimmed);
            }
            normalized.add(trimmed);
        }
        return normalized;
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String requireLabel(String label, String fallback) {
        if (label == null || label.isBlank()) {
            return fallback;
        }
        return label.trim();
    }
}
