package com.pf4erp.core.adapters.in.web.directory;

import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Annuaire léger pour lier User ↔ Employé / assignations (plugins).
 */
@RestController
@RequestMapping("/api/directory")
@Tag(name = "Directory")
public class DirectoryController {

    private final UserAccountRepository userAccountRepository;

    public DirectoryController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lister les utilisateurs (id, username, displayName) pour les liaisons métier")
    public List<UserOptionDto> listUsers() {
        return userAccountRepository.findAll().stream()
                .filter(UserAccount::isEnabled)
                .map(u -> new UserOptionDto(u.getId(), u.getUsername(), u.getDisplayName()))
                .toList();
    }

    public record UserOptionDto(String id, String username, String displayName) {
    }
}
