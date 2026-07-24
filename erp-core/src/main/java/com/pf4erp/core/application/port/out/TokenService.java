package com.pf4erp.core.application.port.out;

import java.time.Instant;
import java.util.Set;

public interface TokenService {

    String issueToken(String username, Set<String> roles, Set<String> permissions);

    TokenClaims parse(String token);

    record TokenClaims(
            String username,
            Set<String> roles,
            Set<String> permissions,
            Instant expiresAt
    ) {
    }
}
