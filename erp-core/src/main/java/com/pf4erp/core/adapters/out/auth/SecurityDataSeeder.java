package com.pf4erp.core.adapters.out.auth;

import com.pf4erp.core.application.port.out.RoleRepository;
import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.CorePermissions;
import com.pf4erp.core.domain.auth.Role;
import com.pf4erp.core.domain.auth.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Seed idempotent des rôles système et utilisateurs démo.
 */
@Component
@Order(1)
public class SecurityDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityDataSeeder(
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRole(
                CorePermissions.ROLE_ADMIN,
                "Administrateur",
                "Accès complet (toutes les permissions actives)",
                Set.of(),
                true
        );
        ensureRole(
                CorePermissions.ROLE_USER,
                "Utilisateur",
                "Accès lecture RH + mes tâches todo",
                Set.of("rh.employe.read", "todo.task.self"),
                true
        );
        ensureRole(
                "TASK_MANAGER",
                "Manager tâches",
                "Gérer projets / tâches todo et assigner aux employés",
                Set.of(
                        "rh.employe.read",
                        "todo.task.self",
                        "todo.project.manage"
                ),
                false
        );

        ensureUser("admin", "Admin ERP", "admin", Set.of(CorePermissions.ROLE_ADMIN));
        ensureUser("user", "Utilisateur RH", "user", Set.of(CorePermissions.ROLE_USER));
        log.info("Security seed ready (roles={}, users={})", roleRepository.count(), userAccountRepository.count());
    }

    private void ensureRole(String code, String label, String description, Set<String> permissions, boolean system) {
        roleRepository.findByCode(code).ifPresentOrElse(
                existing -> {
                    if (CorePermissions.ROLE_ADMIN.equals(code)) {
                        return;
                    }
                    java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(existing.getPermissionCodes());
                    boolean changed = false;
                    for (String permission : permissions) {
                        if (merged.add(permission)) {
                            changed = true;
                        }
                    }
                    if (changed) {
                        roleRepository.save(existing.withPermissionCodes(merged));
                    }
                },
                () -> roleRepository.save(new Role(
                        UUID.randomUUID().toString(),
                        code,
                        label,
                        description,
                        permissions,
                        system
                ))
        );
    }

    private void ensureUser(String username, String displayName, String rawPassword, Set<String> roles) {
        if (userAccountRepository.existsByUsername(username)) {
            return;
        }
        userAccountRepository.save(new UserAccount(
                UUID.randomUUID().toString(),
                username,
                displayName,
                passwordEncoder.encode(rawPassword),
                roles,
                true
        ));
        log.info("Created demo user {}", username);
    }
}
