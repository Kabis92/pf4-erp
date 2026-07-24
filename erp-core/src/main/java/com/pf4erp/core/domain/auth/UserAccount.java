package com.pf4erp.core.domain.auth;

import java.util.Objects;
import java.util.Set;

/**
 * Compte utilisateur. Les permissions effectives sont dérivées des rôles (expand au login).
 */
public final class UserAccount {

    private final String id;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final Set<String> roleCodes;
    private final boolean enabled;

    public UserAccount(
            String id,
            String username,
            String displayName,
            String passwordHash,
            Set<String> roleCodes,
            boolean enabled
    ) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.displayName = Objects.requireNonNull(displayName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.roleCodes = Set.copyOf(roleCodes);
        this.enabled = enabled;
    }

    public UserAccount withDisplayName(String displayName) {
        return new UserAccount(id, username, displayName, passwordHash, roleCodes, enabled);
    }

    public UserAccount withPasswordHash(String passwordHash) {
        return new UserAccount(id, username, displayName, passwordHash, roleCodes, enabled);
    }

    public UserAccount withRoleCodes(Set<String> roleCodes) {
        return new UserAccount(id, username, displayName, passwordHash, roleCodes, enabled);
    }

    public UserAccount withEnabled(boolean enabled) {
        return new UserAccount(id, username, displayName, passwordHash, roleCodes, enabled);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
