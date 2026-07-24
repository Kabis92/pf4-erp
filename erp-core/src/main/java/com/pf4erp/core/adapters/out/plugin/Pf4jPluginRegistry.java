package com.pf4erp.core.adapters.out.plugin;

import com.pf4erp.api.ErpModuleExtension;
import com.pf4erp.core.application.port.out.PermissionCatalogPort;
import com.pf4erp.core.application.port.out.PluginRegistryPort;
import com.pf4erp.core.domain.auth.PermissionDefinition;
import com.pf4erp.core.domain.plugin.PluginDescriptor;
import jakarta.annotation.PreDestroy;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Pf4jPluginRegistry implements PluginRegistryPort, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(Pf4jPluginRegistry.class);

    private final PluginManager pluginManager;
    private final PluginProperties properties;
    private final ApplicationContext parentContext;
    private final PluginControllerRegistrar controllerRegistrar;
    private final PluginSwaggerBridge swaggerBridge;
    private final MongoTemplate mongoTemplate;
    private final PermissionCatalogPort permissionCatalog;

    private final Map<String, RuntimePlugin> runtimePlugins = new ConcurrentHashMap<>();
    private volatile boolean bootstrapped;

    public Pf4jPluginRegistry(
            PluginManager pluginManager,
            PluginProperties properties,
            ApplicationContext parentContext,
            PluginControllerRegistrar controllerRegistrar,
            PluginSwaggerBridge swaggerBridge,
            MongoTemplate mongoTemplate,
            PermissionCatalogPort permissionCatalog
    ) {
        this.pluginManager = pluginManager;
        this.properties = properties;
        this.parentContext = parentContext;
        this.controllerRegistrar = controllerRegistrar;
        this.swaggerBridge = swaggerBridge;
        this.mongoTemplate = mongoTemplate;
        this.permissionCatalog = permissionCatalog;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() != parentContext || bootstrapped) {
            return;
        }
        bootstrapped = true;
        bootstrap();
    }

    private void bootstrap() {
        log.info("Bootstrapping plugins (mode={})", properties.getRuntimeMode());

        if (properties.isDevelopment()) {
            // Important: ne pas loadPlugins() depuis plugins/ — les JAR stale masqueraient target/classes
            for (String fixed : properties.getFixedPluginPath()) {
                Path path = resolvePluginPath(fixed);
                if (path == null) {
                    log.warn("fixed-plugin-path not found: {} (tried cwd and parent)", fixed);
                    continue;
                }
                try {
                    String pluginId = pluginManager.loadPlugin(path);
                    log.info("Loaded development plugin {} from {}", pluginId, path);
                } catch (Exception e) {
                    log.warn("Could not load development plugin from {}: {}", path, e.getMessage());
                }
            }
        } else {
            pluginManager.loadPlugins();
        }

        pluginManager.startPlugins();

        for (PluginWrapper wrapper : pluginManager.getPlugins()) {
            if (wrapper.getPluginState() == PluginState.STARTED) {
                try {
                    startPluginRuntime(wrapper);
                } catch (Exception e) {
                    log.error("Bootstrap failed for plugin {}", wrapper.getPluginId(), e);
                }
            }
        }

        log.info("Started {} plugin runtime(s)", runtimePlugins.size());
    }

    private void startPluginRuntime(PluginWrapper wrapper) {
        String pluginId = wrapper.getPluginId();
        try {
            stopRuntimeOnly(pluginId);

            PluginYamlReader.PluginManifest manifest = readManifest(wrapper);
            List<ErpModuleExtension> extensions = pluginManager.getExtensions(ErpModuleExtension.class, pluginId);

            List<String> uiSlots = new ArrayList<>(manifest.uiSlots());
            List<Class<?>> configs = new ArrayList<>();
            for (ErpModuleExtension extension : extensions) {
                uiSlots.addAll(extension.getUiSlots());
                Class<?>[] springConfigs = extension.getSpringConfigurations();
                if (springConfigs != null) {
                    for (Class<?> config : springConfigs) {
                        configs.add(config);
                    }
                }
            }

            AnnotationConfigApplicationContext child = null;
            if (!configs.isEmpty()) {
                child = new AnnotationConfigApplicationContext();
                child.setParent(parentContext);
                child.setClassLoader(wrapper.getPluginClassLoader());
                child.setDisplayName("plugin-" + pluginId);
                child.getBeanFactory().registerSingleton("mongoTemplate", mongoTemplate);
                for (Class<?> config : configs) {
                    child.register(config);
                }
                child.refresh();

                Map<String, Object> controllers = child.getBeansWithAnnotation(RestController.class);
                controllerRegistrar.registerControllers(pluginId, controllers);
                swaggerBridge.registerControllers(pluginId, controllers, manifest);
                log.info("Plugin {} started with {} controller(s)", pluginId, controllers.size());
            }

            registerPluginPermissions(pluginId, manifest, extensions);

            boolean deletable = isDeletable(wrapper);
            List<String> mergedSlots = uiSlots.stream().distinct().toList();
            String docsUrl = manifest.apiDocsEnabled()
                    ? (manifest.apiDocsUrl() != null
                        ? manifest.apiDocsUrl()
                        : PluginSwaggerBridge.docsUiUrl(manifest.apiDocsGroup() != null ? manifest.apiDocsGroup() : pluginId))
                    : null;
            PluginDescriptor descriptor = new PluginDescriptor(
                    manifest.id() != null ? manifest.id() : pluginId,
                    manifest.version(),
                    manifest.displayName(),
                    manifest.description(),
                    manifest.requires(),
                    manifest.uiEntrypoint(),
                    manifest.uiStyle(),
                    mergedSlots,
                    true,
                    deletable,
                    manifest.apiDocsEnabled(),
                    manifest.apiDocsGroup(),
                    docsUrl
            );

            runtimePlugins.put(pluginId, new RuntimePlugin(descriptor, wrapper, child, deletable));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to start runtime for plugin {}", pluginId, e);
            throw new IllegalStateException("Impossible de démarrer le plugin " + pluginId + ": " + e.getMessage(), e);
        }
    }

    private PluginDescriptor toDescriptor(
            PluginYamlReader.PluginManifest manifest,
            String pluginId,
            boolean started,
            boolean deletable
    ) {
        String group = manifest.apiDocsGroup() != null ? manifest.apiDocsGroup() : pluginId;
        String docsUrl = manifest.apiDocsEnabled()
                ? (manifest.apiDocsUrl() != null ? manifest.apiDocsUrl() : PluginSwaggerBridge.docsUiUrl(group))
                : null;
        return new PluginDescriptor(
                manifest.id() != null ? manifest.id() : pluginId,
                manifest.version(),
                manifest.displayName(),
                manifest.description(),
                manifest.requires(),
                manifest.uiEntrypoint(),
                manifest.uiStyle(),
                uiSlotsDistinct(manifest.uiSlots()),
                started,
                deletable,
                manifest.apiDocsEnabled(),
                manifest.apiDocsGroup(),
                docsUrl
        );
    }

    private List<String> uiSlotsDistinct(List<String> slots) {
        return slots == null ? List.of() : slots.stream().distinct().toList();
    }

    private boolean isDeletable(PluginWrapper wrapper) {
        Path pluginPath = wrapper.getPluginPath();
        if (pluginPath == null) {
            return false;
        }
        Path pluginsDir = Path.of(properties.getPluginsDir()).toAbsolutePath().normalize();
        Path absolute = pluginPath.toAbsolutePath().normalize();
        return absolute.startsWith(pluginsDir) && absolute.toString().endsWith(".jar");
    }

    private PluginYamlReader.PluginManifest readManifest(PluginWrapper wrapper) {
        try (InputStream in = wrapper.getPluginClassLoader().getResourceAsStream("plugin.yaml")) {
            if (in == null) {
                return new PluginYamlReader.PluginManifest(
                        wrapper.getPluginId(),
                        wrapper.getDescriptor().getVersion(),
                        wrapper.getPluginId(),
                        "",
                        "*",
                        "ui/main.js",
                        "ui/style.css",
                        List.of(),
                        false,
                        wrapper.getPluginId(),
                        List.of(),
                        null,
                        List.of()
                );
            }
            return PluginYamlReader.read(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read plugin.yaml for " + wrapper.getPluginId(), e);
        }
    }

    @Override
    public List<PluginDescriptor> listStartedPlugins() {
        return runtimePlugins.values().stream()
                .map(RuntimePlugin::descriptor)
                .filter(PluginDescriptor::isStarted)
                .toList();
    }

    @Override
    public List<PluginDescriptor> listAllPlugins() {
        // Include loaded PF4J plugins even if runtime map incomplete
        Map<String, PluginDescriptor> byId = new ConcurrentHashMap<>();
        for (RuntimePlugin runtime : runtimePlugins.values()) {
            byId.put(runtime.descriptor().getId(), runtime.descriptor());
        }
        for (PluginWrapper wrapper : pluginManager.getPlugins()) {
            byId.computeIfAbsent(wrapper.getPluginId(), id -> {
                PluginYamlReader.PluginManifest manifest = readManifest(wrapper);
                boolean started = wrapper.getPluginState() == PluginState.STARTED
                        && runtimePlugins.containsKey(id)
                        && runtimePlugins.get(id).descriptor().isStarted();
                return toDescriptor(manifest, id, started, isDeletable(wrapper));
            });
        }
        return List.copyOf(byId.values());
    }

    @Override
    public Optional<PluginDescriptor> findById(String pluginId) {
        return Optional.ofNullable(runtimePlugins.get(pluginId)).map(RuntimePlugin::descriptor)
                .or(() -> pluginManager.getPlugins().stream()
                        .filter(w -> w.getPluginId().equals(pluginId))
                        .findFirst()
                        .map(w -> {
                            PluginYamlReader.PluginManifest manifest = readManifest(w);
                            boolean started = w.getPluginState() == PluginState.STARTED
                                    && runtimePlugins.containsKey(pluginId)
                                    && runtimePlugins.get(pluginId).descriptor().isStarted();
                            return toDescriptor(manifest, pluginId, started, isDeletable(w));
                        }));
    }

    @Override
    public Optional<InputStream> openAsset(String pluginId, String relativePath) {
        RuntimePlugin runtime = runtimePlugins.get(pluginId);
        if (runtime == null || !runtime.descriptor().isStarted()) {
            PluginWrapper wrapper = pluginManager.getPlugin(pluginId);
            if (wrapper == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(wrapper.getPluginClassLoader().getResourceAsStream(relativePath));
        }
        return Optional.ofNullable(runtime.wrapper().getPluginClassLoader().getResourceAsStream(relativePath));
    }

    @Override
    public synchronized PluginDescriptor startPlugin(String pluginId) {
        PluginWrapper wrapper = pluginManager.getPlugin(pluginId);
        if (wrapper == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin introuvable: " + pluginId);
        }

        RuntimePlugin existing = runtimePlugins.get(pluginId);
        if (existing != null && existing.descriptor().isStarted()) {
            return existing.descriptor();
        }

        if (wrapper.getPluginState() != PluginState.STARTED) {
            PluginState state = pluginManager.startPlugin(pluginId);
            if (state != PluginState.STARTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Impossible de démarrer le plugin (état=" + state + ")");
            }
        }

        try {
            startPluginRuntime(pluginManager.getPlugin(pluginId));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        RuntimePlugin runtime = runtimePlugins.get(pluginId);
        if (runtime == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Runtime non initialisé");
        }
        return runtime.descriptor();
    }

    @Override
    public synchronized PluginDescriptor stopPlugin(String pluginId) {
        RuntimePlugin runtime = runtimePlugins.get(pluginId);
        PluginWrapper wrapper = pluginManager.getPlugin(pluginId);
        if (wrapper == null && runtime == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin introuvable: " + pluginId);
        }

        stopRuntimeOnly(pluginId);

        if (wrapper != null && wrapper.getPluginState() == PluginState.STARTED) {
            pluginManager.stopPlugin(pluginId);
        }

        wrapper = pluginManager.getPlugin(pluginId);
        if (wrapper == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin introuvable: " + pluginId);
        }

        PluginYamlReader.PluginManifest manifest = readManifest(wrapper);
        boolean deletable = isDeletable(wrapper);
        PluginDescriptor stopped = toDescriptor(manifest, pluginId, false, deletable);
        runtimePlugins.put(pluginId, new RuntimePlugin(stopped, wrapper, null, deletable));
        log.info("Plugin {} stopped", pluginId);
        return stopped;
    }

    @Override
    public synchronized void deletePlugin(String pluginId) {
        PluginWrapper wrapper = pluginManager.getPlugin(pluginId);
        if (wrapper == null) {
            runtimePlugins.remove(pluginId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin introuvable: " + pluginId);
        }

        if (!isDeletable(wrapper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce plugin ne peut pas être supprimé (chemin de développement ou non JAR). "
                            + "Désactivez-le à la place.");
        }

        Path pluginPath = wrapper.getPluginPath().toAbsolutePath().normalize();

        stopRuntimeOnly(pluginId);
        if (wrapper.getPluginState() == PluginState.STARTED) {
            pluginManager.stopPlugin(pluginId);
        }
        pluginManager.unloadPlugin(pluginId);
        runtimePlugins.remove(pluginId);

        try {
            Files.deleteIfExists(pluginPath);
            log.info("Deleted plugin jar {}", pluginPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Plugin déchargé mais fichier non supprimé: " + e.getMessage());
        }
    }

    @Override
    public synchronized PluginDescriptor installPlugin(InputStream jarContent, String originalFilename) {
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".jar")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier doit être un JAR (.jar)");
        }

        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path pluginsDir = Path.of(properties.getPluginsDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(pluginsDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de créer le répertoire plugins: " + e.getMessage());
        }

        Path target = pluginsDir.resolve(safeName).normalize();
        if (!target.startsWith(pluginsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
        }

        if (Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un fichier plugin existe déjà: " + safeName + ". Supprimez-le d’abord ou renommez le JAR.");
        }

        try {
            Files.copy(jarContent, target);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de l’écriture du JAR: " + e.getMessage());
        }

        String loadedPluginId = null;
        try {
            loadedPluginId = pluginManager.loadPlugin(target);
            if (loadedPluginId == null || loadedPluginId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "PF4J n’a pas pu lire le plugin (plugin.properties / Manifest manquant ?)");
            }

            PluginWrapper wrapper = pluginManager.getPlugin(loadedPluginId);
            if (wrapper != null && wrapper.getPluginPath() != null
                    && !wrapper.getPluginPath().toAbsolutePath().normalize().equals(target)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Le plugin « " + loadedPluginId + " » est déjà chargé depuis un autre chemin. "
                                + "Arrêtez / supprimez-le avant de réimporter.");
            }

            PluginDescriptor started = startPlugin(loadedPluginId);
            log.info("Installed and started plugin {} from {}", loadedPluginId, target);
            return started;
        } catch (ResponseStatusException e) {
            rollbackInstall(loadedPluginId, target);
            throw e;
        } catch (Exception e) {
            rollbackInstall(loadedPluginId, target);
            log.error("Plugin install failed for {}", safeName, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Installation échouée: " + e.getMessage());
        }
    }

    private void rollbackInstall(String pluginId, Path target) {
        if (pluginId != null && !pluginId.isBlank()) {
            try {
                stopRuntimeOnly(pluginId);
                PluginWrapper wrapper = pluginManager.getPlugin(pluginId);
                if (wrapper != null && wrapper.getPluginState() == PluginState.STARTED) {
                    pluginManager.stopPlugin(pluginId);
                }
                pluginManager.unloadPlugin(pluginId);
                runtimePlugins.remove(pluginId);
            } catch (Exception e) {
                log.warn("Rollback unload failed for {}: {}", pluginId, e.getMessage());
            }
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // ignore
        }
    }

    private void stopRuntimeOnly(String pluginId) {
        RuntimePlugin runtime = runtimePlugins.get(pluginId);
        controllerRegistrar.unregisterControllers(pluginId);
        swaggerBridge.unregisterControllers(pluginId);
        permissionCatalog.deactivatePlugin(pluginId);
        if (runtime != null && runtime.childContext() != null) {
            try {
                runtime.childContext().close();
            } catch (Exception e) {
                log.warn("Error closing plugin context {}: {}", pluginId, e.getMessage());
            }
        }
    }

    private void registerPluginPermissions(
            String pluginId,
            PluginYamlReader.PluginManifest manifest,
            List<ErpModuleExtension> extensions
    ) {
        Map<String, PermissionDefinition> merged = new LinkedHashMap<>();
        if (manifest.permissions() != null) {
            for (PluginYamlReader.PermissionSpec spec : manifest.permissions()) {
                merged.put(spec.code(), new PermissionDefinition(spec.code(), spec.label(), pluginId, true));
            }
        }
        for (ErpModuleExtension extension : extensions) {
            if (extension.getPermissions() == null) {
                continue;
            }
            for (String code : extension.getPermissions()) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                merged.putIfAbsent(code, new PermissionDefinition(code, code, pluginId, true));
            }
        }
        permissionCatalog.registerPluginPermissions(pluginId, merged.values());
        log.info("Registered {} permission(s) for plugin {}", merged.size(), pluginId);
    }

    /**
     * Résout un chemin plugin relatif que le cwd soit la racine du repo ou erp-core/.
     */
    private Path resolvePluginPath(String fixed) {
        Path direct = Path.of(fixed).toAbsolutePath().normalize();
        if (Files.exists(direct)) {
            return direct;
        }
        Path fromParent = Path.of("..").resolve(fixed).toAbsolutePath().normalize();
        if (Files.exists(fromParent)) {
            return fromParent;
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        for (String pluginId : new ArrayList<>(runtimePlugins.keySet())) {
            stopRuntimeOnly(pluginId);
            runtimePlugins.remove(pluginId);
        }
        pluginManager.stopPlugins();
    }

    private record RuntimePlugin(
            PluginDescriptor descriptor,
            PluginWrapper wrapper,
            AnnotationConfigApplicationContext childContext,
            boolean deletable
    ) {
    }
}
