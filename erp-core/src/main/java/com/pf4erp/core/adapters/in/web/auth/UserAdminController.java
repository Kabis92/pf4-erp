package com.pf4erp.core.adapters.in.web.auth;

import com.pf4erp.core.application.auth.UserAdminUseCase;
import com.pf4erp.core.domain.auth.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - Utilisateurs")
public class UserAdminController {

    private final UserAdminUseCase userAdminUseCase;

    public UserAdminController(UserAdminUseCase userAdminUseCase) {
        this.userAdminUseCase = userAdminUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('core.user.read')")
    @Operation(summary = "Lister les utilisateurs")
    public List<UserDto> list() {
        return userAdminUseCase.list().stream().map(UserDto::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('core.user.read')")
    @Operation(summary = "Détail d’un utilisateur")
    public UserDto get(@PathVariable String id) {
        return UserDto.from(userAdminUseCase.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('core.user.write')")
    @Operation(summary = "Créer un utilisateur")
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return UserDto.from(userAdminUseCase.create(
                request.username(),
                request.displayName(),
                request.password(),
                request.roleCodes(),
                request.enabled() == null || request.enabled()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('core.user.write')")
    @Operation(summary = "Mettre à jour un utilisateur")
    public UserDto update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return UserDto.from(userAdminUseCase.update(
                id,
                request.displayName(),
                request.password(),
                request.roleCodes(),
                request.enabled()
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('core.user.write')")
    @Operation(summary = "Supprimer un utilisateur")
    public void delete(@PathVariable String id, Authentication authentication) {
        userAdminUseCase.delete(id, authentication.getName());
    }

    public record CreateUserRequest(
            @NotBlank String username,
            String displayName,
            @NotBlank String password,
            @NotEmpty Set<String> roleCodes,
            Boolean enabled
    ) {
    }

    public record UpdateUserRequest(
            String displayName,
            String password,
            Set<String> roleCodes,
            Boolean enabled
    ) {
    }

    public record UserDto(
            String id,
            String username,
            String displayName,
            Set<String> roleCodes,
            boolean enabled
    ) {
        static UserDto from(UserAccount user) {
            return new UserDto(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getRoleCodes(),
                    user.isEnabled()
            );
        }
    }
}
