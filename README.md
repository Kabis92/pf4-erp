# pf4-erp

ERP modulaire basé sur **PF4J** (plugins Java) et un shell **React** (micro-frontends). Chaque module métier est un plugin hybride : logique Java + UI compilée livrée dans le même JAR.

## Vision

Construire un ERP extensible où :

- le **Core** orchestre le cycle de vie des plugins, l’auth, et le shell UI ;
- chaque **plugin** encapsule un domaine métier (ex. RH) derrière une architecture hexagonale ;
- le front charge dynamiquement les contributions UI des plugins via des *slots* (modèle proche de [Halo](https://docs.halo.run/developer-guide/plugin/introduction)) ;
- la persistance est abstraite par des **ports** : MongoDB pour le MVP, PostgreSQL possible plus tard sans toucher au domaine.

## Stack

| Couche | Choix |
|--------|--------|
| Backend | Java 21, Spring Boot 3.x, architecture hexagonale (ports & adapters) |
| Contrats plugins | `erp-plugin-api` (PF4J / pf4j-spring) |
| Persistance MVP | MongoDB via adapter secondaire |
| Persistance future | PostgreSQL / JPA derrière les mêmes ports |
| Frontend | React + TypeScript + Vite |
| Design system | **shadcn/ui** + Tailwind + Lucide (port du design erp-ang) |
| UI plugins | IIFE dans `resources/ui/`, React externalisé (globals shell) |

## Prérequis

- JDK 21+
- Maven 3.9+
- Node.js 20+ et npm
- MongoDB 7+ (local ou Docker)

```bash
docker compose up -d
# Mongo URI : mongodb://localhost:27017/pf4erp
# Mongo Express UI : http://localhost:8081
```

## Build & run (MVP)

### 1. UI du plugin RH

```bash
cd plugin-rh/ui
npm install
npm run build
# → plugin-rh/src/main/resources/ui/{main.js,style.css}
```

### 2. Backend (API + Core + plugin)

```bash
# à la racine
mvn -q clean package -DskipTests
# copie plugin-rh.jar → plugins/
```

### 3. Shell React

```bash
cd erp-ui
npm install
npm run dev
# http://localhost:5173 (proxy /api → :8080)
```

### 4. Démarrer le Core

```bash
# mode development : charge plugin-rh/target/classes
java -jar erp-core/target/erp-core-0.1.0-SNAPSHOT.jar

# ou
mvn -pl erp-core spring-boot:run
```

Variables utiles :

| Variable | Défaut | Rôle |
|----------|--------|------|
| `MONGODB_URI` | `mongodb://localhost:27017/pf4erp` | Mongo |
| `JWT_SECRET` | secret démo (≥32 car.) | Signature JWT |
| `PLUGIN_RUNTIME_MODE` | `development` | `development` \| `deployment` |
| `PLUGINS_DIR` | `./plugins` | JARs en mode deployment |
| `PLUGIN_RH_PATH` | `./plugin-rh/target/classes` | Chemin fixe en development |

### Compte démo (seed Mongo)

- `admin` / `admin` — rôle **ADMIN** (toutes les permissions actives du catalogue)
- `user` / `user` — rôle **USER** (`rh.employe.read`, `todo.task.self`) ; seed RH lie ce compte à un employé

Admin : menus **Utilisateurs**, **Rôles & accès**, **Plugins**, **Gestion Todo**.  
Rôle seed **TASK_MANAGER** : manage projets/tâches + lecture employés.  
Les plugins déclarent leurs permissions dans `plugin.yaml` ; elles sont assignables aux rôles.  
Swagger : `/swagger-ui.html` — groupes `core` / `all` + **un groupe par plugin**.

### Critères de succès

- `GET /api/plugins` liste le plugin `rh`
- `GET /api/rh/employes` (Bearer JWT) répond
- UI Vite : login → dashboard (widget compteur) → menu Employés

## Modules

```
pf4-erp/
├── erp-plugin-api/    # Contrats partagés (ErpModuleExtension)
├── erp-core/          # Shell Spring + PF4J + auth JWT + RBAC
├── plugin-rh/         # Module RH (employés + lien userId)
├── plugin-todo/       # Module Todo (projets / tâches assignées)
├── erp-ui/            # Shell React shadcn (PluginLoader, ExtensionSlot)
├── plugins/           # JARs runtime (deployment)
└── docs/
```

## Documentation

| Document | Rôle |
|----------|------|
| [docs/CORE_ARCHITECTURE.md](docs/CORE_ARCHITECTURE.md) | Référence d’architecture |
| [docs/PROMPT_MVP.md](docs/PROMPT_MVP.md) | Prompt maître MVP |
| [docs/DECISIONS.md](docs/DECISIONS.md) | ADR |

## Statut

MVP code en place : Core + plugins RH / Todo + shell React + RBAC.
