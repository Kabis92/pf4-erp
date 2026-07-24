package com.pf4erp.core.adapters.out.plugin;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class Pf4jConfig {

    @Bean
    public PluginManager pluginManager(PluginProperties properties) {
        List<Path> roots = new ArrayList<>();
        roots.add(Path.of(properties.getPluginsDir()).toAbsolutePath().normalize());

        if (properties.isDevelopment()) {
            for (String fixed : properties.getFixedPluginPath()) {
                roots.add(Path.of(fixed).toAbsolutePath().normalize());
            }
        }

        // PF4J DefaultPluginManager uses the first plugins root; additional roots are loaded explicitly later.
        DefaultPluginManager manager = new DefaultPluginManager(roots.getFirst());
        manager.setSystemVersion("0.1.0");
        return manager;
    }
}
