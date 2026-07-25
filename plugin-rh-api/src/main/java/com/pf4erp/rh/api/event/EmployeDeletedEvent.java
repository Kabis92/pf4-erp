package com.pf4erp.rh.api.event;

import com.pf4erp.api.SharedEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Publié par RH après suppression d’un employé.
 */
@SharedEvent
public class EmployeDeletedEvent extends ApplicationEvent {

    private final String employeId;
    private final String userId;

    public EmployeDeletedEvent(Object source, String employeId, String userId) {
        super(source);
        this.employeId = employeId;
        this.userId = userId;
    }

    public String getEmployeId() {
        return employeId;
    }

    /** Peut être {@code null} si l’employé n’était lié à aucun compte. */
    public String getUserId() {
        return userId;
    }
}
