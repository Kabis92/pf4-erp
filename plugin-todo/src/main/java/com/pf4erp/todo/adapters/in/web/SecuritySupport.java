package com.pf4erp.todo.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

final class SecuritySupport {

    private SecuritySupport() {
    }

    @SuppressWarnings("unchecked")
    static String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof Map<?, ?> map)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide");
        }
        Object userId = map.get("userId");
        if (userId == null || userId.toString().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "userId absent du jeton — reconnectez-vous");
        }
        return userId.toString();
    }
}
