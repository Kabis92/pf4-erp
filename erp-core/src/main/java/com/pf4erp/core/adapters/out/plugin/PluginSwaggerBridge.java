package com.pf4erp.core.adapters.out.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.core.customizers.OpenApiBuilderCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.providers.SpringDocProviders;
import org.springdoc.core.providers.SpringWebProvider;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Branche les contrôleurs PF4J dans SpringDoc et expose un groupe OpenAPI
 * <strong>par plugin</strong> à partir de {@code plugin.yaml} ({@code api.docs/group/paths}),
 * sans ajouter de {@code GroupedOpenApi} hardcodé dans le Core.
 */
@Component
public class PluginSwaggerBridge {

    public record PluginApiDocs(
            String pluginId,
            String group,
            String displayName,
            List<String> pathPrefixes
    ) {
    }

    private final Optional<SpringDocProviders> springDocProviders;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;
    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, Object>> controllersByPlugin = new ConcurrentHashMap<>();
    private final Map<String, PluginApiDocs> apiDocsByGroup = new ConcurrentHashMap<>();

    public PluginSwaggerBridge(
            Optional<SpringDocProviders> springDocProviders,
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            ObjectMapper objectMapper
    ) {
        this.springDocProviders = springDocProviders;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.objectMapper = objectMapper;
        ensureCoreSwaggerUrl();
    }

    public void registerControllers(
            String pluginId,
            Map<String, Object> controllers,
            PluginYamlReader.PluginManifest manifest
    ) {
        if (controllers == null || controllers.isEmpty()) {
            unregisterControllers(pluginId);
            return;
        }

        Map<String, Object> prefixed = new HashMap<>();
        controllers.forEach((name, bean) -> {
            prefixed.put("plugin." + pluginId + "." + name, bean);
            AbstractOpenApiResource.addRestControllers(bean.getClass());
        });
        controllersByPlugin.put(pluginId, Map.copyOf(prefixed));

        if (manifest != null && manifest.apiDocsEnabled()) {
            String group = manifest.apiDocsGroup() != null ? manifest.apiDocsGroup() : pluginId;
            List<String> paths = manifest.apiPaths() == null || manifest.apiPaths().isEmpty()
                    ? List.of("/api/" + group + "/**")
                    : List.copyOf(manifest.apiPaths());
            apiDocsByGroup.put(group, new PluginApiDocs(
                    pluginId,
                    group,
                    manifest.displayName() != null ? manifest.displayName() : group,
                    normalizePrefixes(paths)
            ));
        }

        refreshSwaggerUiUrls();
        invalidateSpringDocCaches();
    }

    public void unregisterControllers(String pluginId) {
        Map<String, Object> previous = controllersByPlugin.remove(pluginId);
        if (previous != null) {
            removeAdditionalRestControllers(previous.values());
        }
        apiDocsByGroup.entrySet().removeIf(e -> e.getValue().pluginId().equals(pluginId));
        refreshSwaggerUiUrls();
        invalidateSpringDocCaches();
    }

    public void applyMappings(OpenAPIService openAPIService) {
        Map<String, Object> mappings = openAPIService.getMappingsMap();
        if (mappings == null) {
            return;
        }
        mappings.keySet().removeIf(key -> key.startsWith("plugin."));
        for (Map<String, Object> controllers : controllersByPlugin.values()) {
            mappings.putAll(controllers);
        }
        openAPIService.setCachedOpenAPI(null, Locale.getDefault());
        openAPIService.setCachedOpenAPI(null, Locale.ENGLISH);
        openAPIService.setCachedOpenAPI(null, Locale.FRENCH);
        invalidateSpringDocCaches();
    }

    public Optional<PluginApiDocs> findApiDocs(String group) {
        return Optional.ofNullable(apiDocsByGroup.get(group));
    }

    public List<PluginApiDocs> listApiDocs() {
        return apiDocsByGroup.values().stream()
                .sorted(Comparator.comparing(PluginApiDocs::group))
                .toList();
    }

    public byte[] filterOpenApiForGroup(byte[] allApiDocsJson, String group) throws Exception {
        PluginApiDocs docs = apiDocsByGroup.get(group);
        if (docs == null) {
            return null;
        }
        JsonNode root = objectMapper.readTree(allApiDocsJson);
        if (!(root instanceof ObjectNode objectNode) || !objectNode.has("paths")) {
            return allApiDocsJson;
        }
        ObjectNode paths = (ObjectNode) objectNode.get("paths");
        List<String> toRemove = new ArrayList<>();
        paths.fieldNames().forEachRemaining(path -> {
            if (!matchesAnyPrefix(path, docs.pathPrefixes())) {
                toRemove.add(path);
            }
        });
        toRemove.forEach(paths::remove);
        objectNode.putObject("info")
                .put("title", docs.displayName() + " API")
                .put("version", root.path("info").path("version").asText("0.1.0"));
        return objectMapper.writeValueAsBytes(objectNode);
    }

    public static String docsUiUrl(String group) {
        return "/swagger-ui/index.html?urls.primaryName=" + group;
    }

    private void ensureCoreSwaggerUrl() {
        Set<SwaggerUrl> urls = swaggerUiConfigProperties.getUrls();
        if (urls == null) {
            urls = new LinkedHashSet<>();
            swaggerUiConfigProperties.setUrls(urls);
        }
        urls.removeIf(u -> "core".equals(u.getName()));
        urls.add(new SwaggerUrl("core", "/v3/api-docs/core", "Core"));
    }

    private synchronized void refreshSwaggerUiUrls() {
        ensureCoreSwaggerUrl();
        Set<SwaggerUrl> urls = new LinkedHashSet<>(swaggerUiConfigProperties.getUrls());
        urls.removeIf(u -> u.getName() != null && !"core".equals(u.getName()) && !"all".equals(u.getName()));
        urls.add(new SwaggerUrl("all", "/v3/api-docs/all", "All"));
        for (PluginApiDocs docs : listApiDocs()) {
            urls.add(new SwaggerUrl(
                    docs.group(),
                    "/v3/api-docs/plugins/" + docs.group(),
                    docs.displayName()
            ));
        }
        swaggerUiConfigProperties.setUrls(urls);
        if (swaggerUiConfigProperties.getUrlsPrimaryName() == null
                || "rh".equals(swaggerUiConfigProperties.getUrlsPrimaryName())) {
            swaggerUiConfigProperties.setUrlsPrimaryName("core");
        }
    }

    private static List<String> normalizePrefixes(List<String> paths) {
        return paths.stream()
                .map(p -> p.replace("/**", "").replace("**", ""))
                .map(p -> p.endsWith("/") ? p.substring(0, p.length() - 1) : p)
                .toList();
    }

    private static boolean matchesAnyPrefix(String path, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private void invalidateSpringDocCaches() {
        springDocProviders.flatMap(SpringDocProviders::getSpringWebProvider).ifPresent(this::clearHandlerMethodsCache);
    }

    private void clearHandlerMethodsCache(SpringWebProvider provider) {
        try {
            Field field = SpringWebProvider.class.getDeclaredField("handlerMethods");
            field.setAccessible(true);
            field.set(provider, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to reset SpringDoc handlerMethods cache", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void removeAdditionalRestControllers(Iterable<Object> beans) {
        try {
            Field field = AbstractOpenApiResource.class.getDeclaredField("ADDITIONAL_REST_CONTROLLERS");
            field.setAccessible(true);
            List<Class<?>> additional = (List<Class<?>>) field.get(null);
            for (Object bean : beans) {
                additional.remove(bean.getClass());
            }
        } catch (ReflectiveOperationException ignored) {
            // best-effort
        }
    }
}

@RestController
class PluginOpenApiController {

    private final PluginSwaggerBridge bridge;
    private final ObjectProvider<MultipleOpenApiWebMvcResource> multipleOpenApi;

    PluginOpenApiController(
            PluginSwaggerBridge bridge,
            ObjectProvider<MultipleOpenApiWebMvcResource> multipleOpenApi
    ) {
        this.bridge = bridge;
        this.multipleOpenApi = multipleOpenApi;
    }

    @GetMapping(value = "/v3/api-docs/plugins/{group}", produces = MediaType.APPLICATION_JSON_VALUE)
    public byte[] pluginOpenApi(
            @PathVariable String group,
            HttpServletRequest request,
            Locale locale
    ) throws Exception {
        if (bridge.findApiDocs(group).isEmpty()) {
            return "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Unknown plugin group\",\"version\":\"0\"},\"paths\":{}}".getBytes();
        }
        MultipleOpenApiWebMvcResource resource = multipleOpenApi.getObject();
        byte[] all = resource.openapiJson(request, "/v3/api-docs/all", "all", locale);
        byte[] filtered = bridge.filterOpenApiForGroup(all, group);
        return filtered != null ? filtered : all;
    }
}

@Configuration
class PluginSwaggerConfiguration {

    @Bean
    OpenApiBuilderCustomizer pluginOpenApiBuilderCustomizer(PluginSwaggerBridge bridge) {
        return bridge::applyMappings;
    }

    /** Groupe Core (APIs plateforme). */
    @Bean
    GroupedOpenApi coreApi() {
        return GroupedOpenApi.builder()
                .group("core")
                .displayName("Core")
                .pathsToMatch("/api/auth/**", "/api/plugins/**", "/api/admin/**")
                .build();
    }

    /**
     * Catalogue complet /api/** — base pour dériver les groupes plugins dynamiques
     * sans déclarer un {@code GroupedOpenApi} par module dans le Core.
     */
    @Bean
    GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("All")
                .pathsToMatch("/api/**")
                .build();
    }
}
