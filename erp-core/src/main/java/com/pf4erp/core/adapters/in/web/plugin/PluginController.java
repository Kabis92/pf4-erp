package com.pf4erp.core.adapters.in.web.plugin;

import com.pf4erp.core.application.plugin.PluginQueryUseCase;
import com.pf4erp.core.domain.plugin.PluginDescriptor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginQueryUseCase pluginQueryUseCase;

    public PluginController(PluginQueryUseCase pluginQueryUseCase) {
        this.pluginQueryUseCase = pluginQueryUseCase;
    }

    @GetMapping
    public List<PluginDto> listPlugins() {
        return pluginQueryUseCase.listStartedPlugins().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{pluginId}/assets/**")
    public ResponseEntity<InputStreamResource> asset(
            @PathVariable String pluginId,
            HttpServletRequest request
    ) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String prefix = "/api/plugins/" + pluginId + "/assets/";
        String relativePath = fullPath != null && fullPath.startsWith(prefix)
                ? fullPath.substring(prefix.length())
                : "";

        if (relativePath.isBlank() || relativePath.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        InputStream stream = pluginQueryUseCase.openAsset(pluginId, relativePath).orElse(null);
        if (stream == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = resolveMediaType(relativePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(mediaType)
                .body(new InputStreamResource(stream));
    }

    private PluginDto toDto(PluginDescriptor descriptor) {
        return PluginDto.from(descriptor);
    }

    private MediaType resolveMediaType(String path) {
        if (path.endsWith(".js") || path.endsWith(".mjs")) {
            return MediaType.valueOf("text/javascript");
        }
        if (path.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        }
        if (path.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }
        if (path.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public record PluginDto(
            String id,
            String version,
            String displayName,
            String description,
            String requires,
            String uiEntrypoint,
            String uiStyle,
            List<String> uiSlots,
            boolean started,
            boolean deletable,
            boolean apiDocsEnabled,
            String apiDocsGroup,
            String apiDocsUrl,
            String entry
    ) {
        public static PluginDto from(PluginDescriptor d) {
            return new PluginDto(
                    d.getId(),
                    d.getVersion(),
                    d.getDisplayName(),
                    d.getDescription(),
                    d.getRequires(),
                    d.getUiEntrypoint(),
                    d.getUiStyle(),
                    d.getUiSlots(),
                    d.isStarted(),
                    d.isDeletable(),
                    d.isApiDocsEnabled(),
                    d.getApiDocsGroup(),
                    d.getApiDocsUrl(),
                    "/api/plugins/" + d.getId() + "/assets/" + d.getUiEntrypoint()
                            + "?v=" + d.getVersion()
            );
        }
    }
}
