package com.pf4erp.core.application.auth;

import com.pf4erp.core.application.port.out.PermissionCatalogPort;
import com.pf4erp.core.application.port.out.RoleRepository;
import com.pf4erp.core.application.port.out.TokenService;
import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.CorePermissions;
import com.pf4erp.core.domain.auth.PermissionDefinition;
import com.pf4erp.core.domain.auth.Role;
import com.pf4erp.core.domain.auth.UserAccount;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthUseCase {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PermissionCatalogPort permissionCatalog;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthUseCase(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PermissionCatalogPort permissionCatalog,
            TokenService tokenService,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.permissionCatalog = permissionCatalog;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResult login(String username, String password) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Identifiants invalides"));

        if (!user.isEnabled()) {
            throw new AuthenticationException("Compte désactivé");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Identifiants invalides");
        }

        Set<String> permissions = resolvePermissions(user);
        String token = tokenService.issueToken(
                user.getId(),
                user.getUsername(),
                user.getRoleCodes(),
                permissions
        );
        return new AuthResult(token, toUserInfo(user, permissions));
    }

    public UserInfo me(String username) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Utilisateur introuvable"));
        if (!user.isEnabled()) {
            throw new AuthenticationException("Compte désactivé");
        }
        return toUserInfo(user, resolvePermissions(user));
    }

    /**
     * ADMIN reçoit toutes les permissions actives du catalogue.
     * Les autres rôles : union des codes assignés (conservés même si plugin inactif ;
     * les authorities JWT n’incluent que les permissions encore actives).
     */
    public Set<String> resolvePermissions(UserAccount user) {
        if (user.getRoleCodes().contains(CorePermissions.ROLE_ADMIN)) {
            return permissionCatalog.listActive().stream()
                    .map(PermissionDefinition::getCode)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        List<Role> roles = roleRepository.findByCodes(user.getRoleCodes());
        Set<String> granted = new HashSet<>();
        for (Role role : roles) {
            granted.addAll(role.getPermissionCodes());
        }

        Set<String> active = permissionCatalog.listActive().stream()
                .map(PermissionDefinition::getCode)
                .collect(Collectors.toSet());

        return granted.stream()
                .filter(active::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private UserInfo toUserInfo(UserAccount user, Set<String> permissions) {
        return new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCodes(),
                permissions
        );
    }

    public record AuthResult(String token, UserInfo user) {
    }

    public record UserInfo(
            String id,
            String username,
            String displayName,
            Set<String> roles,
            Set<String> permissions
    ) {
    }

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(Objects.requireNonNull(message));
        }
    }
}
