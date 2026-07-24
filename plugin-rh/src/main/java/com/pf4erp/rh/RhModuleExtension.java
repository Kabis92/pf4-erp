package com.pf4erp.rh;

import com.pf4erp.api.ErpModuleExtension;
import com.pf4erp.rh.config.RhPluginConfiguration;
import org.pf4j.Extension;

import java.util.List;
import java.util.Set;

@Extension
public class RhModuleExtension implements ErpModuleExtension {

    @Override
    public String getModuleId() {
        return "rh";
    }

    @Override
    public String getDisplayName() {
        return "Ressources Humaines";
    }

    @Override
    public Set<String> getPermissions() {
        return Set.of("rh.employe.read", "rh.employe.write");
    }

    @Override
    public List<String> getUiSlots() {
        return List.of("dashboard.widgets", "menu.main");
    }

    @Override
    public Class<?>[] getSpringConfigurations() {
        return new Class<?>[]{RhPluginConfiguration.class};
    }
}
