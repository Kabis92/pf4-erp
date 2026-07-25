# Communication inter-plugins (style Halo)

Guide développeur pour partager des données et réagir entre modules pf4-erp.
Inspiré de la [doc Halo — interaction plugins](https://docs.halo.run/developer-guide/plugin/interaction/shared-events).

## Pourquoi pas un gros `common` métier ?

Les contextes Spring des plugins sont **isolés** (enfants du Core). Un bean RH n’est **pas** injectable dans Todo.

On partage donc des **contrats** (`*-api`), pas des implémentations :

| Module | Contenu autorisé | Interdit |
|--------|------------------|----------|
| `erp-plugin-api` | Contrats Core (`ErpModuleExtension`, `@SharedEvent`, `ExtensionGetter`) | Métier RH/Todo |
| `plugin-rh-api` | DTO + `EmployeDirectory` + events RH | Mongo, controllers, use cases |
| `plugin-rh` | Implémentation | — |
| `plugin-todo` | Consomme `plugin-rh-api` en `provided` | Dépendre de `plugin-rh` JAR |

```
┌──────────────┐     ExtensionGetter      ┌──────────────┐
│ plugin-todo  │ ───────────────────────► │ plugin-rh    │
│ (consommateur)│   EmployeDirectory       │ (fournisseur)│
└──────┬───────┘                          └──────┬───────┘
       │ @EventListener                            │ publishEvent
       │                                           │
       └──────────── SharedEventHub (Core) ────────┘
```

---

### Approche UI — facade HTTP du module consommateur

Le front d’un plugin **ne doit pas** appeler l’API CRUD d’un autre module pour un besoin métier local
(ex. Todo Gestion qui listait `/api/rh/employes`).

Préférer une **facade** dans le module consommateur :

- UI Todo → `GET /api/todo/assignees` (permission `todo.project.manage`)
- Backend Todo → `EmployeDirectory.listLinkedToUsers()` via `ExtensionGetter`

Appeler l’API d’un autre plugin depuis le front reste acceptable (style Halo) pour **naviguer / afficher**
ce module, pas pour encapsuler un cas d’usage Todo.

---

## Approche 1 — Module API du fournisseur (`plugin-*-api`)

### Concept

Comme Halo `plugin-a-api` : un JAR **léger** publié avec le fournisseur, consommé en `provided` / `compileOnly` par les autres plugins.

### Créer un nouvel API (ex. futur `plugin-facturation-api`)

1. Module Maven `plugin-xxx-api` (dépend de `erp-plugin-api` + `pf4j`).
2. Y mettre uniquement : records DTO, interfaces `ExtensionPoint`, classes d’événements `@SharedEvent`.
3. Le Core dépend de cet API (classpath parent → même `Class` pour tous les plugins).
4. Le consommateur déclare :

```xml
<dependency>
  <groupId>com.pf4erp</groupId>
  <artifactId>plugin-xxx-api</artifactId>
  <scope>provided</scope>
</dependency>
```

5. Dans `plugin.yaml` du consommateur :

```yaml
pluginDependencies:
  xxx: ">=0.1.0"
```

### Exemple concret RH

- `EmployeRef` — DTO stable
- `EmployeDirectory` — port d’extension
- `EmployeLinkedToUserEvent` / `Unlinked` / `Deleted` — events

---

## Approche 2 — Extension points (appel synchrone)

### Concept

Halo : interface `extends ExtensionPoint` + découverte via `ExtensionGetter`.

Chez nous : le fournisseur expose un **bean Spring** du type de l’interface ; le Core agrège les beans des contextes plugins.

### Fournir une extension (RH)

```java
@Bean
public EmployeDirectory employeDirectory(EmployeRepository repo) {
    return new SpringEmployeDirectory(repo);
}
```

### Consommer (Todo)

```java
@Bean
public EmployeDirectoryPort employeDirectoryPort(ExtensionGetter extensionGetter) {
    return new ExtensionEmployeDirectoryAdapter(extensionGetter);
}
```

`ExtensionGetter` est un bean **Core** (parent) : injectable dans tout contexte enfant.

```java
extensionGetter.getExtension(EmployeDirectory.class)
    .orElseThrow(() -> new IllegalStateException("Plugin RH requis"));
```

### Quand l’utiliser

- Besoin **immédiat** d’une donnée (résoudre userId → employeId).
- Contrat stable, lecture / commande synchrone.

### À éviter

- Lire la collection Mongo d’un autre plugin (`rh_employes` depuis Todo) — anti-pattern remplacé par cette approche.

---

## Approche 3 — Événements partagés `@SharedEvent` (asynchrone)

### Concept

Chaque plugin a son propre `ApplicationContext`. Un `publishEvent` local **ne traverse pas** les frères.

Le Core fournit `SharedEventHub` : tout `ApplicationEvent` annoté `@SharedEvent` publié dans un plugin est relayé vers le Core puis les autres plugins.

### Définir un event (dans `*-api`)

```java
@SharedEvent
public class EmployeDeletedEvent extends ApplicationEvent {
    // champs immuables + getters
}
```

### Publier (uniquement le propriétaire)

```java
eventPublisher.publishEvent(new EmployeDeletedEvent(this, employeId, userId));
```

Règle Halo : **ne republier jamais** l’event d’un autre plugin.

### Écouter (consommateur)

```java
@EventListener
public void onDeleted(EmployeDeletedEvent event) {
    // réagir : cache, désassignation, notification…
}
```

Enregistrer le listener comme `@Bean` (nos plugins n’activent pas le component-scan automatique).

### Bonnes pratiques

| Règle | Pourquoi |
|-------|----------|
| Nommer au passé (`…Deleted`, `…Linked`) | L’état a déjà changé |
| Listeners légers / `@Async` si long | Ne pas bloquer le bus |
| Payload minimal (ids + libellés utiles) | Éviter de coupler aux entités internes |
| Un seul éditeur par type d’event | Pas de boucles A↔B |

### Quand l’utiliser

- Réagir à un **changement** (lien user, suppression).
- Découpler (Todo n’a pas besoin d’appeler RH au moment du delete si un listener suffit).

---

## Checklist nouveau couplage inter-modules

1. [ ] Créer / enrichir `plugin-fournisseur-api`
2. [ ] ExtensionPoint **ou** `@SharedEvent` (ou les deux)
3. [ ] Implémenter dans le plugin fournisseur (bean + publish)
4. [ ] Consommateur : `provided` sur `*-api` + `pluginDependencies`
5. [ ] Adapter hexagonal local si besoin (port applicatif → ExtensionGetter)
6. [ ] Documenter l’event / l’extension dans ce fichier ou un ADR

---

## Fichiers de référence

| Rôle | Chemin |
|------|--------|
| Annotation bus | `erp-plugin-api/.../SharedEvent.java` |
| Getter extensions | `erp-plugin-api/.../ExtensionGetter.java` |
| Hub Core | `erp-core/.../SharedEventHub.java` |
| API RH | `plugin-rh-api/` |
| Impl RH directory | `plugin-rh/.../SpringEmployeDirectory.java` |
| Adapter Todo | `plugin-todo/.../ExtensionEmployeDirectoryAdapter.java` |
| Listener exemple | `plugin-todo/.../RhEmployeEventListener.java` |

Voir aussi [CORE_ARCHITECTURE.md](CORE_ARCHITECTURE.md) et [DECISIONS.md](DECISIONS.md) ADR-008.
