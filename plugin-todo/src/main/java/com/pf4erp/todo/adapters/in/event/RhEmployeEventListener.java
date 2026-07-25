package com.pf4erp.todo.adapters.in.event;

import com.pf4erp.rh.api.event.EmployeDeletedEvent;
import com.pf4erp.rh.api.event.EmployeLinkedToUserEvent;
import com.pf4erp.rh.api.event.EmployeUnlinkedFromUserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

/**
 * Exemple de consommation d’événements {@code @SharedEvent} RH depuis Todo.
 * Les listeners reçoivent les events via le {@code SharedEventHub} du Core.
 */
public class RhEmployeEventListener {

    private static final Logger log = LoggerFactory.getLogger(RhEmployeEventListener.class);

    @EventListener
    public void onLinked(EmployeLinkedToUserEvent event) {
        log.info(
                "Todo a reçu EmployeLinkedToUserEvent employeId={} userId={} nom={}",
                event.getEmployeId(),
                event.getUserId(),
                event.getEmployeNom()
        );
    }

    @EventListener
    public void onUnlinked(EmployeUnlinkedFromUserEvent event) {
        log.info(
                "Todo a reçu EmployeUnlinkedFromUserEvent employeId={} previousUserId={}",
                event.getEmployeId(),
                event.getPreviousUserId()
        );
    }

    @EventListener
    public void onDeleted(EmployeDeletedEvent event) {
        log.info(
                "Todo a reçu EmployeDeletedEvent employeId={} userId={} (piste: désassigner les tâches)",
                event.getEmployeId(),
                event.getUserId()
        );
    }
}
