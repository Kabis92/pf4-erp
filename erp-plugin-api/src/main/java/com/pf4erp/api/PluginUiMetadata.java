package com.pf4erp.api;

/**
 * Métadonnées UI d'un plugin (issues de plugin.yaml).
 */
public record PluginUiMetadata(
        String entrypoint,
        String style
) {
    public static PluginUiMetadata defaults() {
        return new PluginUiMetadata("ui/main.js", "ui/style.css");
    }
}
