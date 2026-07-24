package com.pf4erp.core.application.auth;

import com.pf4erp.core.application.port.out.TokenService;
import com.pf4erp.core.application.port.out.UserAccountRepository;
import com.pf4erp.core.domain.auth.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
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
        UserAccountRepository users = username -> {
            if ("admin".equals(username)) {
                return Optional.of(new UserAccount(
                        "1", "admin", "Admin", encoder.encode("admin"),
                        Set.of("ADMIN"), Set.of("rh.employe.read")
                ));
            }
            return Optional.empty();
        };
        TokenService tokens = new TokenService() {
            @Override
            public String issueToken(String username, Set<String> roles, Set<String> permissions) {
                return "token-" + username;
            }

            @Override
            public TokenClaims parse(String token) {
                return new TokenClaims("admin", Set.of("ADMIN"), Set.of(), Instant.now().plusSeconds(60));
            }
        };
        authUseCase = new AuthUseCase(users, tokens, encoder);
    }

    @Test
    void login_success() {
        AuthUseCase.AuthResult result = authUseCase.login("admin", "admin");
        assertEquals("token-admin", result.token());
        assertEquals("admin", result.user().username());
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
