package com.pf4erp.core.adapters.out.plugin;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enregistre / désenregistre les contrôleurs REST des plugins sur le DispatcherServlet du Core.
 */
@Component
public class PluginControllerRegistrar {

    private final RequestMappingHandlerMapping handlerMapping;
    private final Map<String, List<RequestMappingInfo>> registered = new ConcurrentHashMap<>();

    public PluginControllerRegistrar(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public void registerControllers(String pluginId, Map<String, Object> beans) {
        unregisterControllers(pluginId);
        List<RequestMappingInfo> infos = new ArrayList<>();

        for (Object bean : beans.values()) {
            Class<?> type = bean.getClass();
            if (!AnnotatedElementUtils.hasAnnotation(type, RestController.class)
                    && !AnnotatedElementUtils.hasAnnotation(type, RequestMapping.class)) {
                continue;
            }

            RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);

            for (Method method : type.getMethods()) {
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) {
                    continue;
                }

                RequestMappingInfo info = buildMapping(typeMapping, methodMapping);
                handlerMapping.registerMapping(info, bean, method);
                infos.add(info);
            }
        }

        registered.put(pluginId, infos);
    }

    public void unregisterControllers(String pluginId) {
        List<RequestMappingInfo> infos = registered.remove(pluginId);
        if (infos != null) {
            for (RequestMappingInfo info : infos) {
                handlerMapping.unregisterMapping(info);
            }
        }
    }

    private RequestMappingInfo buildMapping(RequestMapping typeMapping, RequestMapping methodMapping) {
        RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(handlerMapping.getPatternParser());

        RequestMappingInfo methodInfo = toInfo(methodMapping, options);
        if (typeMapping == null) {
            return methodInfo;
        }
        return toInfo(typeMapping, options).combine(methodInfo);
    }

    private RequestMappingInfo toInfo(RequestMapping mapping, RequestMappingInfo.BuilderConfiguration options) {
        String[] paths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        return RequestMappingInfo.paths(paths)
                .methods(mapping.method())
                .params(mapping.params())
                .headers(mapping.headers())
                .consumes(mapping.consumes())
                .produces(mapping.produces())
                .options(options)
                .build();
    }
}
