package com.pf4erp.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque un {@link org.springframework.context.ApplicationEvent} comme partageable
 * entre le Core et les plugins (bus d’événements inter-contextes).
 *
 * <p>Inspiré de Halo {@code @SharedEvent} :
 * <a href="https://docs.halo.run/developer-guide/plugin/interaction/shared-events">événements partagés</a>.
 *
 * <p>Règles :
 * <ul>
 *   <li>Seul le plugin (ou le Core) qui <strong>définit</strong> l’événement doit le publier.</li>
 *   <li>Les listeners doivent rester légers ; préférer {@code @Async} si le traitement est long.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SharedEvent {
}
