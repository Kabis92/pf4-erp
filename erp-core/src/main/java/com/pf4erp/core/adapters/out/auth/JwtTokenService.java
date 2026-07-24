package com.pf4erp.core.adapters.out.auth;

import com.pf4erp.core.application.port.out.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtTokenService implements TokenService {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenService(
            @Value("${pf4erp.jwt.secret}") String secret,
            @Value("${pf4erp.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("pf4erp.jwt.secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String issueToken(String username, Set<String> roles, Set<String> permissions) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(username)
                .claim("roles", List.copyOf(roles))
                .claim("permissions", List.copyOf(permissions))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new TokenClaims(
                claims.getSubject(),
                toStringSet(claims.get("roles")),
                toStringSet(claims.get("permissions")),
                claims.getExpiration().toInstant()
        );
    }

    @SuppressWarnings("unchecked")
    private Set<String> toStringSet(Object value) {
        if (value instanceof Collection<?> collection) {
            Set<String> result = new HashSet<>();
            for (Object item : collection) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return Set.copyOf(result);
        }
        return Set.of();
    }
}
