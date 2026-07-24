package com.pf4erp.core.adapters.in.web.security;

import com.pf4erp.core.application.port.out.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                TokenService.TokenClaims claims = tokenService.parse(token);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                claims.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                claims.permissions().forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.username(),
                        null,
                        authorities
                );
                authentication.setDetails(Map.of(
                        "userId", claims.userId() != null ? claims.userId() : "",
                        "username", claims.username()
                ));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
