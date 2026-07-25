package com.pf4erp.rh.api.event;

import com.pf4erp.api.SharedEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Publié par RH lorsqu’un lien employé ↔ utilisateur est retiré.
 */
@SharedEvent
public class EmployeUnlinkedFromUserEvent extends ApplicationEvent {

    private final String employeId;
    private final String previousUserId;

    public EmployeUnlinkedFromUserEvent(Object source, String employeId, String previousUserId) {
        super(source);
        this.employeId = employeId;
        this.previousUserId = previousUserId;
    }

    public String getEmployeId() {
        return employeId;
    }

    public String getPreviousUserId() {
        return previousUserId;
    }
}
