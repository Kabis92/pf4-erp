package com.pf4erp.core.application.auth;

import com.pf4erp.core.application.port.out.PermissionCatalogPort;
import com.pf4erp.core.domain.auth.PermissionDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionQueryUseCase {

    private final PermissionCatalogPort permissionCatalog;

    public PermissionQueryUseCase(PermissionCatalogPort permissionCatalog) {
        this.permissionCatalog = permissionCatalog;
    }

    public List<PermissionDefinition> listAll() {
        return permissionCatalog.listAll();
    }
}
