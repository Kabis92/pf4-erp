package com.pf4erp.core.application.auth;

import com.pf4erp.core.application.port.out.RoleRepository;
import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdminUseCase {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminUseCase(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccount> list() {
        return userAccountRepository.findAll();
    }

    public UserAccount get(String id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public UserAccount create(String username, String displayName, String password, Set<String> roleCodes, boolean enabled) {
        String normalized = normalizeUsername(username);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom d’utilisateur requis");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe requis");
        }
        if (userAccountRepository.existsByUsername(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nom d’utilisateur déjà utilisé");
        }
        Set<String> roles = validateRoles(roleCodes);
        UserAccount user = new UserAccount(
                UUID.randomUUID().toString(),
                normalized,
                requireDisplayName(displayName, normalized),
                passwordEncoder.encode(password),
                roles,
                enabled
        );
        return userAccountRepository.save(user);
    }

    public UserAccount update(
            String id,
            String displayName,
            String newPassword,
            Set<String> roleCodes,
            Boolean enabled
    ) {
        UserAccount existing = get(id);
        String name = displayName != null ? requireDisplayName(displayName, existing.getUsername()) : existing.getDisplayName();
        Set<String> roles = roleCodes != null ? validateRoles(roleCodes) : existing.getRoleCodes();
        boolean isEnabled = enabled != null ? enabled : existing.isEnabled();
        UserAccount updated = existing
                .withDisplayName(name)
                .withRoleCodes(roles)
                .withEnabled(isEnabled);
        if (newPassword != null && !newPassword.isBlank()) {
            updated = updated.withPasswordHash(passwordEncoder.encode(newPassword));
        }
        return userAccountRepository.save(updated);
    }

    public void delete(String id, String currentUsername) {
        UserAccount existing = get(id);
        if (existing.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte");
        }
        userAccountRepository.deleteById(id);
    }

    private Set<String> validateRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un rôle est requis");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String code : roleCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String upper = code.trim().toUpperCase(Locale.ROOT);
            if (roleRepository.findByCode(upper).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle inconnu: " + upper);
            }
            normalized.add(upper);
        }
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un rôle est requis");
        }
        return normalized;
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireDisplayName(String displayName, String fallback) {
        if (displayName == null || displayName.isBlank()) {
            return fallback;
        }
        return displayName.trim();
    }
}
