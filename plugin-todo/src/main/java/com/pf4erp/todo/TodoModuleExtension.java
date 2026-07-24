package com.pf4erp.todo;

import com.pf4erp.api.ErpModuleExtension;
import com.pf4erp.todo.config.TodoPluginConfiguration;
import org.pf4j.Extension;

import java.util.List;
import java.util.Set;

@Extension
public class TodoModuleExtension implements ErpModuleExtension {

    @Override
    public String getModuleId() {
        return "todo";
    }

    @Override
    public String getDisplayName() {
        return "Todo";
    }

    @Override
    public Set<String> getPermissions() {
        return Set.of("todo.task.self", "todo.project.manage");
    }

    @Override
    public List<String> getUiSlots() {
        return List.of("menu.main");
    }

    @Override
    public Class<?>[] getSpringConfigurations() {
        return new Class<?>[]{TodoPluginConfiguration.class};
    }
}
