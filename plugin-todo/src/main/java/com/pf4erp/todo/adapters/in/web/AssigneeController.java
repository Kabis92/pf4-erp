package com.pf4erp.todo.adapters.in.web;

import com.pf4erp.todo.application.port.EmployeDirectoryPort;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Facade Todo pour l’UI Gestion : le front n’appelle pas {@code /api/rh/**}.
 */
@RestController
@RequestMapping("/api/todo/assignees")
@Tag(name = "Todo - Assignables")
public class AssigneeController {

    private final EmployeDirectoryPort employeDirectoryPort;

    public AssigneeController(EmployeDirectoryPort employeDirectoryPort) {
        this.employeDirectoryPort = employeDirectoryPort;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('todo.project.manage')")
    public List<AssigneeDto> list() {
        try {
            return employeDirectoryPort.listAssignable().stream()
                    .map(a -> new AssigneeDto(a.id(), a.nom()))
                    .toList();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }
    }

    public record AssigneeDto(String id, String nom) {
    }
}
