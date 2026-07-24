package com.pf4erp.todo.application.port;

import java.util.Optional;

/**
 * Lecture du lien Employé RH (collection Mongo rh_employes) sans dépendre du JAR RH.
 */
public interface EmployeDirectoryPort {
    Optional<String> findEmployeIdByUserId(String userId);

    Optional<String> findEmployeNameById(String employeId);
}
