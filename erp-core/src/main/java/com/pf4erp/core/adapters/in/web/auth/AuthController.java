package com.pf4erp.core.adapters.in.web.auth;

import com.pf4erp.core.application.auth.AuthUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthUseCase.AuthResult result = authUseCase.login(request.username(), request.password());
            return new LoginResponse(result.token(), toDto(result.user()));
        } catch (AuthUseCase.AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        try {
            return toDto(authUseCase.me(authentication.getName()));
        } catch (AuthUseCase.AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    private UserDto toDto(AuthUseCase.UserInfo user) {
        return new UserDto(
                user.id(),
                user.username(),
                user.displayName(),
                user.roles(),
                user.permissions()
        );
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(String token, UserDto user) {
    }

    public record UserDto(
            String id,
            String username,
            String displayName,
            Set<String> roles,
            Set<String> permissions
    ) {
    }
}
