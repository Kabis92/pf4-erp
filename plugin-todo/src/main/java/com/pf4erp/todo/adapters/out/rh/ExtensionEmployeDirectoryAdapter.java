package com.pf4erp.todo.adapters.out.rh;

import com.pf4erp.api.ExtensionGetter;
import com.pf4erp.rh.api.EmployeDirectory;
import com.pf4erp.rh.api.EmployeRef;
import com.pf4erp.todo.application.port.EmployeDirectoryPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur Todo → extension point RH via {@link ExtensionGetter} (pas d’accès Mongo direct).
 */
public class ExtensionEmployeDirectoryAdapter implements EmployeDirectoryPort {

    private final ExtensionGetter extensionGetter;

    public ExtensionEmployeDirectoryAdapter(ExtensionGetter extensionGetter) {
        this.extensionGetter = Objects.requireNonNull(extensionGetter);
    }

    @Override
    public Optional<String> findEmployeIdByUserId(String userId) {
        return directory().findByUserId(userId).map(EmployeRef::id);
    }

    @Override
    public Optional<String> findEmployeNameById(String employeId) {
        return directory().findById(employeId).map(EmployeRef::nom);
    }

    @Override
    public List<AssigneeRef> listAssignable() {
        return directory().listLinkedToUsers().stream()
                .map(ref -> new AssigneeRef(ref.id(), ref.nom()))
                .toList();
    }

    private EmployeDirectory directory() {
        return extensionGetter.getExtension(EmployeDirectory.class)
                .orElseThrow(() -> new IllegalStateException(
                        "Extension EmployeDirectory indisponible — le plugin RH doit être démarré"
                ));
    }
}
