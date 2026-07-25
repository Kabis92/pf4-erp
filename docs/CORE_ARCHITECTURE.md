# Architecture Core — pf4-erp

Document de référence pour l’architecture de l’ERP modulaire PF4J + React. Aligné avec [PROMPT_MVP.md](PROMPT_MVP.md) et [DECISIONS.md](DECISIONS.md).

## 1. Principes

### Architecture hexagonale

Chaque module Java (`erp-core` et chaque plugin) isole le domaine des détails techniques :

- le **domaine** ne connaît ni Spring, ni MongoDB, ni HTTP ;
- les **use cases** dépendent uniquement de **ports** (interfaces) ;
- les **adapters** branchent le monde extérieur (REST, Mongo, PF4J) sur ces ports.

### Extensibilité PF4J

Les modules métier sont des plugins PF4J. Le Core charge, démarre et isole chaque plugin via son ClassLoader. Un plugin peut contribuer :

- des extensions Java (API PF4J) ;
- un contexte Spring enfant ;
- des assets UI (JS/CSS) embarqués dans le JAR ;
- un manifeste `plugin.yaml` (métadonnées, version, entrypoint UI).

### Micro-frontends

Le shell React (`erp-ui`) découvre les plugins actifs et charge dynamiquement leurs bundles ES Module. Les contributions UI s’accrochent à des **ExtensionSlot** (ex. `dashboard.widgets`, `menu.main`).

### Isolation ClassLoader

Chaque plugin dispose d’un ClassLoader enfant PF4J. Les dépendances partagées (Spring, API Core) restent dans le parent ; le code métier et ses libs spécifiques restent dans le plugin. Objectif : éviter les collisions de versions et permettre le hot-reload (MVP : reload au redémarrage accepté).

```
┌─────────────────────────────────────────────────────────┐
│  Adapters in (REST, PF4J)                               │
│       │                                                 │
│       ▼                                                 │
│  Application (use cases) ──► Domain (entités / VO)      │
│       │                                                 │
│       ▼                                                 │
│  Ports driven (repositories, events…)                   │
│       │                                                 │
│       ▼                                                 │
│  Adapters out (Mongo MVP · PostgreSQL futur)            │
└─────────────────────────────────────────────────────────┘
```

## 2. Structure multi-module Maven

```
pf4-erp/
├── pom.xml                          # parent
├── erp-plugin-api/                  # contrats Core (ExtensionPoint, SharedEvent, ExtensionGetter)
├── plugin-rh-api/                   # contrats publics RH (EmployeRef, EmployeDirectory, events)
├── erp-core/                        # application Spring Boot + PF4J + SharedEventHub
├── plugin-rh/                       # plugin métier RH (JAR PF4J + ui/)
├── plugin-todo/                     # plugin Todo (consomme plugin-rh-api)
├── erp-ui/                          # shell React + TypeScript (Vite + shadcn)
└── docs/
```

`erp-plugin-api` joue le rôle de la *shared suite* Halo (socle Core).  
Chaque fournisseur métier peut exposer un **`plugin-*-api`** (contrats uniquement) — voir [PLUGIN_INTERACTION.md](PLUGIN_INTERACTION.md).

### Convention de packages hexagonaux

Appliquée à `erp-core` et à chaque plugin (ex. `plugin-rh`), avec le préfixe de package du module :

| Package | Contenu |
|---------|---------|
| `…domain/` | Entités, value objects, règles métier, exceptions domaine |
| `…application/` | Use cases, ports *driving* et *driven* |
| `…adapters.in.web/` | Contrôleurs REST, DTO HTTP, mapping in → application |
| `…adapters.out.persistence.mongo/` | Documents Mongo, repositories Spring Data, mapping out |

Exemple pour `plugin-rh` :

```
com.pf4erp.rh.domain/
com.pf4erp.rh.application/          # EmployeUseCase, EmployeRepository (port)
com.pf4erp.rh.adapters.in.web/      # EmployeController
com.pf4erp.rh.adapters.out.persistence.mongo/  # MongoEmployeRepository
```

Exemple pour `erp-core` :

```
com.pf4erp.core.domain/
com.pf4erp.core.application/
com.pf4erp.core.adapters.in.web/    # PluginController, AssetController, AuthController
com.pf4erp.core.adapters.out.persistence.mongo/
```

Règle : **aucune** annotation Spring Data / Mongo / Jakarta Persistence dans `domain/` ou `application/` (hors éventuellement stéréotypes applicatifs purs si nécessaires — préférer des interfaces pures).

## 3. Persistance

### Ports repository

Les use cases dépendent d’interfaces dans `application/` (ou `domain/` si purement métier), par exemple :

```java
public interface EmployeRepository {
    Optional<Employe> findById(EmployeId id);
    List<Employe> findAll();
    Employe save(Employe employe);
    void delete(EmployeId id);
}
```

L’identifiant métier est un value object (`EmployeId` avec `String` ou UUID), **pas** un `ObjectId` Mongo.

### Adapter Mongo (MVP)

Implémentation dans `adapters.out.persistence.mongo/` :

- documents annotés `@Document` ;
- `MongoRepository` / `MongoTemplate` ;
- mapper document ↔ entité domaine.

Le Core et les plugins câblent cet adapter dans leur contexte Spring (Core parent ou contexte enfant du plugin).

### Anti-fuite technique

Interdit dans le domaine et les use cases :

- `org.bson.types.ObjectId`
- `@Document`, `@Field`, `@Id` (Spring Data Mongo)
- `MongoTemplate`, `MongoClient`
- URL/connection strings

Autorisé uniquement dans `adapters.out.persistence.mongo/`.

### Chemin PostgreSQL (futur)

Un nouvel adapter `adapters.out.persistence.jpa/` (ou `…postgres/`) implémente les **mêmes ports**. Aucun changement attendu dans `domain/` ni dans les use cases. Migrations : Flyway (ou Liquibase) **dans cet adapter**, pas dans le domaine.

## 4. Backend Core (`erp-core`)

### Rôles

- Démarrer Spring Boot et le `SpringPluginManager` (pf4j-spring).
- Charger les plugins selon le **mode runtime** (voir ci-dessous).
- Créer un **contexte Spring enfant** par plugin démarré ; y enregistrer les beans du plugin (use cases + adapters).
- Exposer l’auth JWT et les endpoints d’infrastructure.
- Lire `plugin.yaml` pour exposer métadonnées + entrypoint UI au shell.

### Modes runtime plugins (inspirés Halo)

| Mode | Comportement |
|------|----------------|
| `deployment` (défaut) | Charge les JAR depuis un répertoire configurable (ex. `./plugins`) |
| `development` | Charge aussi des chemins fixes de projets plugins (équivalent Halo `fixed-plugin-path`) pour itérer sans repackager systématiquement |

Configuration indicative :

```yaml
pf4erp:
  plugin:
    runtime-mode: deployment   # ou development
    plugins-dir: ./plugins
    fixed-plugin-path: []      # chemins absolus en development
```

### Endpoints (adapters in)

| Méthode | Chemin | Rôle |
|---------|--------|------|
| `GET` | `/api/plugins` | Liste des plugins démarrés (+ métadonnées UI : id, version, entrypoint) |
| `GET` | `/api/plugins/{pluginId}/assets/**` | Sert les assets sous `resources/ui/` du JAR (`main.js`, `style.css`) |
| `POST` | `/api/auth/login` | Authentification → JWT |
| `GET` | `/api/auth/me` | Utilisateur courant + rôles |

Les endpoints métier (ex. employés) vivent dans les **adapters in du plugin**, montés via le contexte enfant (préfixe recommandé : `/api/rh/...`).

### Cycle de vie plugin

1. PF4J résout et charge le plugin.
2. Le Core crée un `AnnotationConfigApplicationContext` enfant.
3. Le plugin enregistre sa configuration (`@Configuration`) : ports ← adapters Mongo, controllers.
4. Les contrôleurs du plugin sont exposés (DispatcherServlet partagé ou registration explicite — à figer à l’implémentation, critère : isolation + routing clair).

## 5. Structure plugin type Halo

Référence : [structure Halo](https://docs.halo.run/developer-guide/plugin/basics/structure), [manifest](https://docs.halo.run/developer-guide/plugin/basics/manifest). On reprend la forme, pas le modèle de données CRD.

### Arborescence d’un plugin

```
plugin-rh/
├── ui/                              # Front du plugin (Vite library, React TS)
│   ├── src/
│   │   ├── index.ts                 # entry : register routes + slots
│   │   ├── views/
│   │   └── components/
│   ├── package.json
│   └── vite.config.ts
├── src/main/java/...                # hexagone Java + classe Plugin PF4J
└── src/main/resources/
    ├── plugin.yaml                  # manifeste obligatoire
    └── ui/                          # sortie du build Vite (embarqué dans le JAR)
        ├── main.js
        └── style.css
```

### `plugin.yaml` (MVP)

Champs minimaux attendus :

- `id` / `metadata.name` — identifiant unique (ex. `rh`)
- `version`
- `displayName`, `description`
- `requires` — plage de versions Core (SemVer)
- `ui.entrypoint` — chemin relatif des assets (ex. `ui/main.js`)

Le Core agrège ces infos dans `GET /api/plugins`.

### Extension points

| Couche | Mécanisme |
|--------|-----------|
| Java | Interfaces dans `erp-plugin-api` + `@Extension` PF4J (ex. `ErpModuleExtension`) |
| Front | Slots nommés multi-instance : `dashboard.widgets`, `menu.main` ; le module UI appelle `registerExtension(slot, component)` |

## 6. Frontend (`erp-ui`) — shell design

### Source de design

Le look & IA du shell viennent du projet Angular **erp-ang** (hors monorepo), portés en React — **pas** Angular dans pf4-erp :

| erp-ang | erp-ui |
|---------|--------|
| ZardUI + Tailwind 4 + Lucide | **shadcn/ui** + Tailwind + **lucide-react** |
| Tokens OKLCH `neutral`, `--radius: 0.625rem`, famille `sidebar-*` | Mêmes tokens dans `globals.css` / thème Tailwind |
| Sidebar 260px + header breadcrumb + footer | Même chrome |
| `member-list` | Pattern de référence pour la page Employés RH |

Références de lecture (chemin local développeur) :

- tokens : `erp-ang/src/tailwind.css`
- layout : `erp-ang/src/app/layouts/{main-layout,content-layout,sidebar,header,footer}`
- menu : `erp-ang/src/app/layouts/sidebar/sidebar-menu.config.ts`
- liste : `erp-ang/src/app/features/members/pages/member-list`

### Layout chrome

```
┌──────────────────┬─────────────────────────────────────┐
│ SIDEBAR 260px    │ HEADER (breadcrumb + notifications) │
│ brand + menu     ├─────────────────────────────────────┤
│ + user / logout  │ MAIN (dashboard + routes plugins)   │
│                  ├─────────────────────────────────────┤
│                  │ FOOTER (copyright + version)        │
└──────────────────┴─────────────────────────────────────┘
```

- Viewport `h-dvh`, overflow contrôlé dans les panneaux.
- Menu : base Core + contributions dynamiques via slot `menu.main`.
- Auth : écran **Login JWT** (absent d’erp-ang — à ajouter dans le shell).

### Primitives shadcn (MVP)

Button, Input, Badge, Avatar, Breadcrumb ; Table et/ou Dropdown si besoin pour la liste employés.

### Concepts plugin loader

| Concept | Rôle |
|---------|------|
| `PluginDefinition` | Métadonnée : `id`, `version`, `entry` (URL du module ES), `extensions` (slots) |
| `PluginLoader` | Appelle `GET /api/plugins`, puis `import()` dynamique de chaque entry |
| `ExtensionSlot` | Composant React qui rend les contributions enregistrées pour un nom de slot |

### Shared dependencies

Vite externalise React, ReactDOM (et éventuellement le design system du shell). Les bundles plugins importent ces libs depuis le runtime du shell (import map ou globals), pour éviter N copies de React.

### Flux typique

1. Shell démarre → login JWT.
2. `PluginLoader` récupère la liste des plugins.
3. Pour chaque plugin, charge `/api/plugins/{id}/assets/ui/main.js` (ou chemin déclaré dans `plugin.yaml`).
4. Le module plugin enregistre routes + `registerExtension(slot, component)`.
5. `ExtensionSlot name="dashboard.widgets"` affiche le widget RH ; le menu intègre les liens `menu.main`.

## 7. Exemple : `plugin-rh`

Plugin hybride livré en un JAR PF4J :

```
plugin-rh.jar
├── classes/                 # hexagone Java + Plugin PF4J
├── plugin.yaml
└── ui/
    ├── main.js              # bundle ES Module React
    └── style.css
```

### Domaine RH (MVP)

- Entité `Employe` (nom, email, poste, date d’embauche).
- Port `EmployeRepository`.
- Use cases : lister, créer, mettre à jour, supprimer.
- Adapter in : `EmployeController` sous `/api/rh/employes`.
- Adapter out : `MongoEmployeRepository`.
- UI : page liste (pattern member-list) + widget dashboard (compteur) sur `dashboard.widgets` + entrée menu sur `menu.main`.

Le Core **n’importe jamais** `MongoEmployeRepository` ; il ne connaît que le cycle de vie PF4J et les contrats `erp-plugin-api`.

## 8. Sécurité RBAC

- **Authentification** : JWT émis et validé par `erp-core`.
- **Autorisation** : `@PreAuthorize(hasAuthority('…'))` sur les adapters in (Core + plugins).
- **Modèle** : User (Mongo) → Role(s) → Permission codes. Les permissions ne sont pas stockées sur l’utilisateur.
- **ADMIN** : reçoit dynamiquement **toutes** les permissions actives du catalogue au login.
- **Catalogue** : permissions Core (`core.user.*`, `core.role.*`, `core.plugin.admin`) + permissions déclarées par les plugins (`plugin.yaml` + `ErpModuleExtension.getPermissions()`). Au stop d’un plugin, ses permissions passent `inactive` mais restent visibles.
- **Admin UI** : `/users`, `/roles`, `/plugins` (filtrés par permissions).

- Pas de logique d’auth dans le domaine pur : les règles « qui peut faire quoi » passent par la couche application ou l’adapter in.

## 9. Bonnes pratiques

### Dépendances

- Dépendances **métier / spécifiques** → dans le plugin (ClassLoader plugin).
- Contrats Core → `erp-plugin-api` ; contrats d’un fournisseur → `plugin-<id>-api` (consommateurs en `provided`).
- Communication inter-plugins → ExtensionPoint + `@SharedEvent` ([PLUGIN_INTERACTION.md](PLUGIN_INTERACTION.md)) ; **interdit** : lecture Mongo croisée, dépendance au JAR d’implémentation d’un autre plugin.
- Éviter que le Core dépende d’un plugin d’implémentation (les `*-api` sur le classpath parent sont OK).

### Migrations Mongo

- Scripts / changelogs **versionnés** (ex. Mongock, ou scripts JSON/JS numérotés) dans l’adapter Mongo ou un module `…persistence.mongo.migrations`.
- Ne jamais migrer depuis le domaine.

### PostgreSQL futur

- Nouvel adapter derrière les mêmes ports.
- **Flyway** (ou Liquibase) attaché à cet adapter uniquement.
- Feature-flag ou profil Spring pour choisir l’implémentation du port (`mongo` vs `jpa`).

### Tests

- Tests unitaires domaine / use cases avec fakes des ports.
- Tests d’intégration adapters Mongo (Testcontainers).
- Tests UI shell avec mocks de `PluginLoader`.

### Ce que cette architecture n’est pas

- Pas de moteur XML type Axelor au MVP.
- Pas de schéma Halo CRD / JSONB comme modèle de données.
- Pas d’Angular dans le runtime : le design erp-ang est **porté** en React + shadcn.
