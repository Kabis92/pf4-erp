package com.pf4erp.rh.api.event;

import com.pf4erp.api.SharedEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Publié par RH lorsqu’un employé est lié à un compte utilisateur.
 * À publier uniquement depuis plugin-rh.
 */
@SharedEvent
public class EmployeLinkedToUserEvent extends ApplicationEvent {

    private final String employeId;
    private final String userId;
    private final String employeNom;

    public EmployeLinkedToUserEvent(Object source, String employeId, String userId, String employeNom) {
        super(source);
        this.employeId = employeId;
        this.userId = userId;
        this.employeNom = employeNom;
    }

    public String getEmployeId() {
        return employeId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmployeNom() {
        return employeNom;
    }
}
