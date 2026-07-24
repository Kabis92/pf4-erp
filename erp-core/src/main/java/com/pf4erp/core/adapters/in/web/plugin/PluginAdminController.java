package com.pf4erp.core.adapters.in.web.plugin;

import com.pf4erp.core.application.plugin.PluginQueryUseCase;
import com.pf4erp.core.domain.plugin.PluginDescriptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/plugins")
@PreAuthorize("hasAuthority('core.plugin.admin')")
@Tag(name = "Admin - Plugins")
public class PluginAdminController {

    private final PluginQueryUseCase pluginQueryUseCase;

    public PluginAdminController(PluginQueryUseCase pluginQueryUseCase) {
        this.pluginQueryUseCase = pluginQueryUseCase;
    }

    @GetMapping
    @Operation(summary = "Lister tous les plugins (admin)")
    public List<PluginController.PluginDto> listAll() {
        return pluginQueryUseCase.listAllPlugins().stream()
                .map(PluginController.PluginDto::from)
                .toList();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Importer un plugin (JAR) puis le démarrer")
    public PluginController.PluginDto importPlugin(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier JAR requis");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "plugin.jar";
        try {
            return PluginController.PluginDto.from(
                    pluginQueryUseCase.install(file.getInputStream(), filename)
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible: " + e.getMessage());
        }
    }

    @PostMapping("/{pluginId}/start")
    @Operation(summary = "Activer / démarrer un plugin")
    public PluginController.PluginDto start(@PathVariable String pluginId) {
        return PluginController.PluginDto.from(pluginQueryUseCase.start(pluginId));
    }

    @PostMapping("/{pluginId}/stop")
    @Operation(summary = "Désactiver / arrêter un plugin")
    public PluginController.PluginDto stop(@PathVariable String pluginId) {
        return PluginController.PluginDto.from(pluginQueryUseCase.stop(pluginId));
    }

    @DeleteMapping("/{pluginId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un plugin (JAR dans plugins/)")
    public void delete(@PathVariable String pluginId) {
        pluginQueryUseCase.delete(pluginId);
    }

    @GetMapping("/{pluginId}")
    @Operation(summary = "Détail d’un plugin")
    public PluginController.PluginDto get(@PathVariable String pluginId) {
        PluginDescriptor descriptor = pluginQueryUseCase.getPlugin(pluginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin introuvable"));
        return PluginController.PluginDto.from(descriptor);
    }
}
