package com.pf4erp.core.adapters.out.plugin;

import org.pf4j.BasePluginLoader;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginClasspath;
import org.pf4j.PluginLoader;
import org.pf4j.PluginManager;
import org.pf4j.RuntimeMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class Pf4jConfig {

    @Bean
    public PluginManager pluginManager(PluginProperties properties) {
        Path pluginsRoot = Path.of(properties.getPluginsDir()).toAbsolutePath().normalize();
        boolean development = properties.isDevelopment();

        DefaultPluginManager manager = new DefaultPluginManager(pluginsRoot) {
            @Override
            public RuntimeMode getRuntimeMode() {
                return development ? RuntimeMode.DEVELOPMENT : RuntimeMode.DEPLOYMENT;
            }

            @Override
            protected PluginLoader createPluginLoader() {
                if (!development) {
                    return super.createPluginLoader();
                }
                // PF4J DevelopmentPluginLoader attend la racine du module Maven et résout
                // {root}/target/classes. Nos fixed-plugin-path pointent déjà vers target/classes
                // (où se trouve plugin.properties) → on ajoute "." au classpath du plugin.
                PluginClasspath classpath = new PluginClasspath().addClassesDirectories(".");
                return new BasePluginLoader(this, classpath);
            }
        };
        manager.setSystemVersion("0.1.0");
        return manager;
    }
}
