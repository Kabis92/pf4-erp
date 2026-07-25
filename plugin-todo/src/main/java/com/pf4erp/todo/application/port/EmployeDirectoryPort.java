package com.pf4erp.todo.application.port;

import java.util.List;
import java.util.Optional;

/**
 * Port applicatif Todo pour résoudre un employé RH.
 * Implémenté via {@link com.pf4erp.todo.adapters.out.rh.ExtensionEmployeDirectoryAdapter}
 * (extension point {@code EmployeDirectory}, pas lecture Mongo directe).
 */
public interface EmployeDirectoryPort {

    Optional<String> findEmployeIdByUserId(String userId);

    Optional<String> findEmployeNameById(String employeId);

    /** Assignables = employés liés à un compte utilisateur. */
    List<AssigneeRef> listAssignable();

    record AssigneeRef(String id, String nom) {
    }
}
