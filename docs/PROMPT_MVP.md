# Spécification MVP — pf4-erp

Cahier des charges du premier incrément livrable. Aligné avec [CORE_ARCHITECTURE.md](CORE_ARCHITECTURE.md) et [DECISIONS.md](DECISIONS.md).

---

## Prompt (copier depuis la ligne suivante)

```
Tu travailles dans le dépôt pf4-erp. L’objectif est un MVP solide, exécutable et testable d’un ERP modulaire.

## Objectif MVP

Livrer une application qui démarre en local et démontre :

1. Un Core Spring Boot + PF4J qui charge un plugin métier (manifeste plugin.yaml).
2. Un plugin RH (employés) avec API REST, persistance MongoDB, et UI hybride (ui/ → resources/ui/).
3. Un shell React (shadcn) qui se connecte (JWT), reprend le design erp-ang, charge dynamiquement l’UI du plugin RH, et affiche un widget dashboard + une page employés.

Critères de succès : `mvn package`, démarrage Core avec Mongo, plugin RH actif, UI Vite qui consomme `/api/plugins` et `/api/rh/employes`.

## Stack imposée (ne pas dévier)

- Java 21, Spring Boot 3.x, Maven multi-module
- Architecture hexagonale (ports & adapters) dans erp-core ET dans chaque plugin
- Module erp-plugin-api (contrats PF4J / ErpModuleExtension uniquement)
- PF4J / pf4j-spring pour les plugins
- MongoDB via adapter secondaire uniquement (Spring Data Mongo dans adapters.out)
- React + TypeScript + Vite pour erp-ui
- UI kit shell : shadcn/ui + Tailwind + lucide-react
- JWT pour l’auth dans le Core

Interdit au MVP : Angular runtime, Axelor, JSON Forms, moteur XML, schéma type Halo CRD/JSONB, JPA/PostgreSQL (prévoir seulement les ports pour un futur adapter).

## Design shell (obligatoire)

Porter le look & IA du projet Angular erp-ang en React — ne pas intégrer Angular.

Références locales (lecture seule, hors monorepo) :
- Tokens : projet de référence `erp-ang` (hors dépôt), fichier `src/tailwind.css`
  (OKLCH, baseColor neutral, --radius 0.625rem, variables sidebar-*)
- Layout : erp-ang/src/app/layouts/{main-layout,content-layout,sidebar,header,footer}
- Menu : erp-ang/src/app/layouts/sidebar/sidebar-menu.config.ts
- Pattern liste : erp-ang/src/app/features/members/pages/member-list

Chrome attendu :
┌──────────────────┬─────────────────────────────────────┐
│ SIDEBAR 260px    │ HEADER (breadcrumb + notifications) │
│ brand + menu     ├─────────────────────────────────────┤
│ + user / logout  │ MAIN (dashboard + routes plugins)   │
│                  ├─────────────────────────────────────┤
│                  │ FOOTER (copyright + version)        │
└──────────────────┴─────────────────────────────────────┘

- Viewport h-dvh ; tokens OKLCH copiés dans le thème Tailwind / globals.css
- Primitives shadcn MVP : Button, Input, Badge, Avatar, Breadcrumb (+ Table/Dropdown si besoin)
- Écran Login JWT (absent d’erp-ang — à créer)
- Page Employés calquée sur member-list (toolbar, recherche/filtres, table card, pagination)
- Menu = items Core + contributions slot menu.main des plugins

## Arborescence attendue

pf4-erp/
├── pom.xml
├── erp-plugin-api/
├── erp-core/
├── plugin-rh/
│   ├── ui/                         # Vite library React TS
│   └── src/main/resources/
│       ├── plugin.yaml
│       └── ui/                     # main.js + style.css (sortie build)
├── erp-ui/
├── plugins/                        # répertoire runtime des JAR (deployment)
└── docs/                           # déjà présent : ne pas écraser sans raison

## Conventions hexagonales (obligatoires)

Packages par module (préfixe com.pf4erp.core / com.pf4erp.rh) :

- …domain/                         — entités, VO, règles ; ZÉRO Spring/Mongo/HTTP
- …application/                    — use cases + ports (driving et driven)
- …adapters.in.web/                — contrôleurs REST uniquement
- …adapters.out.persistence.mongo/ — documents Mongo, repositories, mappers

Règles :
- Pas d’ObjectId, @Document, MongoTemplate dans domain/ ou application/
- Identifiants métier = value objects (String/UUID), mapping ObjectId uniquement dans l’adapter Mongo
- Controllers et Mongo uniquement dans les adapters

## Structure plugin type Halo (obligatoire)

- Classe Plugin PF4J + ErpModuleExtension (erp-plugin-api)
- plugin.yaml : id, version, displayName, requires, ui.entrypoint
- Build ui/ → src/main/resources/ui/main.js (+ style.css)
- Modes Core : deployment (./plugins) et development (fixed-plugin-path)
- Slots front : dashboard.widgets, menu.main

## Livrables code

### 1. erp-plugin-api

- Interface ErpModuleExtension (métadonnées plugin, slots exposés)
- Contrats partagés minimaux (pas de Spring Data)

### 2. erp-core

- Spring Boot app + SpringPluginManager
- Chargement plugins : deployment (plugins-dir) + development (fixed-plugin-path)
- Lecture plugin.yaml ; contexte Spring enfant par plugin démarré
- Endpoints adapters in :
  - GET  /api/plugins
  - GET  /api/plugins/{pluginId}/assets/**
  - POST /api/auth/login
  - GET  /api/auth/me
- Sécurité JWT (utilisateur démo acceptable pour le MVP)
- RBAC minimal : rôles/permissions sur use cases ou @PreAuthorize

### 3. plugin-rh

- Plugin PF4J hybride (JAR) avec ui/ + plugin.yaml + resources/ui/
- Domaine : Employe (nom, email, poste, date d’embauche)
- Port : EmployeRepository
- Use cases : CRUD / liste
- EmployeController sous /api/rh/employes
- MongoEmployeRepository dans adapters.out.persistence.mongo
- UI : widget compteur (dashboard.widgets) + page employés (pattern member-list) + entrée menu.main

### 4. erp-ui (Shell React)

- Design erp-ang porté (shadcn + tokens + layout ci-dessus)
- Login JWT
- PluginDefinition, PluginLoader (GET /api/plugins + import() dynamique)
- ExtensionSlot (dashboard.widgets, menu.main)
- React / ReactDOM externalisés pour les bundles plugins
- Dashboard avec slots ; routing vers pages plugin

### 5. Build & run

Documenter :
- build Vite plugin-rh/ui → resources/ui/
- build Maven (api + Core + plugin), copie JAR vers plugins/
- build Vite erp-ui
- variables : Mongo URI, JWT secret, plugins-dir, runtime-mode
- commande de démarrage

## Ordre de génération (respecter strictement)

1. erp-plugin-api + ports/domain (core minimal si besoin, puis rh)
2. Use cases (application) avec ports
3. Adapters Core (web auth/plugins/assets, sécurité, PluginManager, lecture plugin.yaml)
4. Plugin RH Java (adapters in/out + packaging PF4J + plugin.yaml)
5. plugin-rh/ui Vite (bundle → resources/ui/) + enregistrement slots
6. erp-ui Vite (shell shadcn fidèle erp-ang, auth, layout)
7. PluginLoader frontend + branchement réel sur les assets du plugin

À chaque étape : code compilable, pas de stubs abandonnés sans TODO clair.

## Qualité

- Français ou anglais OK pour le code (noms cohérents) ; commentaires utiles seulement si non évidents
- Tests unitaires use cases avec fake repository
- Pas de digressions hors scope
- Aligne-toi sur docs/CORE_ARCHITECTURE.md si présent

Commence par proposer l’arborescence des packages puis implémente dans l’ordre ci-dessus.
```

---

## Notes d’usage

- Coller le bloc ci-dessus tel quel dans un nouveau chat Cursor Agent, à la racine de `pf4-erp`.
- Garder [CORE_ARCHITECTURE.md](CORE_ARCHITECTURE.md) ouvert ou référencé pour éviter la dérive.
- Pour le design, laisser l’agent lire les chemins erp-ang indiqués (hors repo) ; ne pas copier le projet Angular dans pf4-erp.
- Ne pas élargir le MVP (pas de second plugin, pas de Postgres) tant que le flux Core → plugin-rh → UI n’est pas démontré de bout en bout.
