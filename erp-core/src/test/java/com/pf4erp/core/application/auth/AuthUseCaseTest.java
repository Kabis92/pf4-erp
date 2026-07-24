package com.pf4erp.core.application.auth;

import com.pf4erp.core.application.port.out.PermissionCatalogPort;
import com.pf4erp.core.application.port.out.RoleRepository;
import com.pf4erp.core.application.port.out.TokenService;
import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.PermissionDefinition;
import com.pf4erp.core.domain.auth.Role;
import com.pf4erp.core.domain.auth.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUseCaseTest {

    private AuthUseCase authUseCase;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserAccountRepository users = new UserAccountRepository() {
            @Override
            public Optional<UserAccount> findById(String id) {
                return findByUsername("admin").filter(u -> u.getId().equals(id));
            }

            @Override
            public Optional<UserAccount> findByUsername(String username) {
                if ("admin".equals(username)) {
                    return Optional.of(new UserAccount(
                            "1", "admin", "Admin", encoder.encode("admin"),
                            Set.of("ADMIN"), true
                    ));
                }
                return Optional.empty();
            }

            @Override
            public List<UserAccount> findAll() {
                return List.of();
            }

            @Override
            public UserAccount save(UserAccount user) {
                return user;
            }

            @Override
            public void deleteById(String id) {
            }

            @Override
            public boolean existsByUsername(String username) {
                return "admin".equals(username);
            }

            @Override
            public long count() {
                return 1;
            }
        };

        RoleRepository roles = new RoleRepository() {
            @Override
            public Optional<Role> findById(String id) {
                return Optional.empty();
            }

            @Override
            public Optional<Role> findByCode(String code) {
                return Optional.empty();
            }

            @Override
            public List<Role> findAll() {
                return List.of();
            }

            @Override
            public List<Role> findByCodes(Iterable<String> codes) {
                return List.of();
            }

            @Override
            public Role save(Role role) {
                return role;
            }

            @Override
            public void deleteById(String id) {
            }

            @Override
            public boolean existsByCode(String code) {
                return false;
            }

            @Override
            public long count() {
                return 0;
            }
        };

        PermissionCatalogPort catalog = new PermissionCatalogPort() {
            @Override
            public void ensureCorePermissions() {
            }

            @Override
            public void registerPluginPermissions(String pluginId, Collection<PermissionDefinition> permissions) {
            }

            @Override
            public void deactivatePlugin(String pluginId) {
            }

            @Override
            public List<PermissionDefinition> listAll() {
                return listActive();
            }

            @Override
            public List<PermissionDefinition> listActive() {
                return List.of(new PermissionDefinition("rh.employe.read", "read", "rh", true));
            }

            @Override
            public Optional<PermissionDefinition> findByCode(String code) {
                return Optional.of(new PermissionDefinition(code, code, null, true));
            }
        };

        TokenService tokens = new TokenService() {
            @Override
            public String issueToken(String userId, String username, Set<String> roles, Set<String> permissions) {
                return "token-" + username;
            }

            @Override
            public TokenClaims parse(String token) {
                return new TokenClaims("1", "admin", Set.of("ADMIN"), Set.of(), Instant.now().plusSeconds(60));
            }
        };
        authUseCase = new AuthUseCase(users, roles, catalog, tokens, encoder);
    }

    @Test
    void login_success() {
        AuthUseCase.AuthResult result = authUseCase.login("admin", "admin");
        assertEquals("token-admin", result.token());
        assertEquals("admin", result.user().username());
        assertTrue(result.user().permissions().contains("rh.employe.read"));
    }

    @Test
    void login_invalidPassword() {
        assertThrows(AuthUseCase.AuthenticationException.class, () -> authUseCase.login("admin", "wrong"));
    }

    @Test
    void me_returnsUser() {
        assertTrue(authUseCase.me("admin").permissions().contains("rh.employe.read"));
    }
}
