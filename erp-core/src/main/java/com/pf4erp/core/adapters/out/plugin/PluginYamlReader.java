package com.pf4erp.core.adapters.out.plugin;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Lecture minimale de plugin.yaml (style Halo).
 */
public final class PluginYamlReader {

    private PluginYamlReader() {
    }

    @SuppressWarnings("unchecked")
    public static PluginManifest read(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(inputStream);
        if (root == null) {
            throw new IllegalArgumentException("plugin.yaml is empty");
        }

        String id = firstNonBlank(
                asString(root.get("id")),
                asString(nested(root, "metadata", "name"))
        );
        String version = firstNonBlank(asString(root.get("version")), "0.0.0");
        String displayName = firstNonBlank(asString(root.get("displayName")), id);
        String description = asString(root.get("description"));
        String requires = firstNonBlank(asString(root.get("requires")), "*");

        Map<String, Object> ui = root.get("ui") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Collections.emptyMap();
        String entrypoint = firstNonBlank(asString(ui.get("entrypoint")), "ui/main.js");
        String style = firstNonBlank(asString(ui.get("style")), "ui/style.css");

        List<String> slots = List.of();
        if (ui.get("slots") instanceof List<?> list) {
            slots = list.stream().map(String::valueOf).toList();
        }

        Map<String, Object> api = root.get("api") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Collections.emptyMap();
        boolean docsEnabled = Boolean.TRUE.equals(api.get("docs")) || "true".equalsIgnoreCase(asString(api.get("docs")));
        String docsGroup = firstNonBlank(asString(api.get("group")), id);
        List<String> apiPaths = new ArrayList<>();
        if (api.get("paths") instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    apiPaths.add(String.valueOf(item));
                }
            }
        }
        String docsUrl = firstNonBlank(
                asString(api.get("docsUrl")),
                docsEnabled ? "/swagger-ui/index.html?urls.primaryName=" + docsGroup : null
        );

        List<PermissionSpec> permissions = parsePermissions(root.get("permissions"));

        return new PluginManifest(
                id, version, displayName, description, requires,
                entrypoint, style, slots,
                docsEnabled, docsGroup, List.copyOf(apiPaths), docsUrl,
                List.copyOf(permissions)
        );
    }

    @SuppressWarnings("unchecked")
    private static List<PermissionSpec> parsePermissions(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<PermissionSpec> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String code) {
                if (!code.isBlank()) {
                    result.add(new PermissionSpec(code.trim(), code.trim()));
                }
            } else if (item instanceof Map<?, ?> map) {
                Map<String, Object> m = (Map<String, Object>) map;
                String code = asString(m.get("code"));
                if (code == null || code.isBlank()) {
                    continue;
                }
                String label = firstNonBlank(asString(m.get("label")), code.trim());
                result.add(new PermissionSpec(code.trim(), label));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object nested(Map<String, Object> root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
        }
        return current;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record PermissionSpec(String code, String label) {
    }

    public record PluginManifest(
            String id,
            String version,
            String displayName,
            String description,
            String requires,
            String uiEntrypoint,
            String uiStyle,
            List<String> uiSlots,
            boolean apiDocsEnabled,
            String apiDocsGroup,
            List<String> apiPaths,
            String apiDocsUrl,
            List<PermissionSpec> permissions
    ) {
    }
}
