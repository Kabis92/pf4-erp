# Décisions d’architecture (ADR léger)

Décisions figées pour pf4-erp. Complète [CORE_ARCHITECTURE.md](CORE_ARCHITECTURE.md) et [PROMPT_MVP.md](PROMPT_MVP.md).

---

## ADR-001 — React plutôt qu’Angular

**Statut :** Accepté

**Contexte :** Le shell doit charger dynamiquement l’UI des plugins (ES Modules, slots d’extension). Angular impose un runtime et un modèle de compilation moins adaptés au chargement à la demande de micro-frontends légers. Un shell Angular de référence (`erp-ang`) existe déjà pour le design.

**Décision :** React + TypeScript + Vite pour `erp-ui` et pour les bundles UI des plugins. Le **design** d’erp-ang est **porté** en React (shadcn/ui) ; Angular n’entre pas dans le runtime pf4-erp.

**Conséquences :** `PluginLoader` + `import()` dynamique ; externalisation de React/ReactDOM ; pas d’Angular au MVP ni dans les prompts de génération ; tokens/layout erp-ang comme référence visuelle uniquement.

---

## ADR-002 — Architecture hexagonale Spring Boot

**Statut :** Accepté

**Contexte :** Core et plugins doivent rester testables et interchangeables côté infra (persistance, HTTP) sans couplage au framework dans le métier.

**Décision :** Architecture hexagonale (ports & adapters) dans `erp-core` et dans chaque plugin, avec les packages :

- `domain/`
- `application/`
- `adapters.in.web/`
- `adapters.out.persistence.mongo/`

**Conséquences :** Domaine sans Spring/Mongo/HTTP ; controllers et Mongo uniquement en adapters ; mêmes conventions Core et plugins pour réduire la charge cognitive.

---

## ADR-003 — Inspiration Halo limitée aux plugins hybrides

**Statut :** Accepté

**Contexte :** Halo illustre bien les plugins Java + UI embarquée, PF4J et ClassLoaders. Son modèle de données (CRD / JSONB, etc.) n’est pas requis pour cet ERP.

**Décision :** Reprendre le modèle **hybride** (JAR = Java + assets JS) et l’isolation PF4J. Ne **pas** adopter le schéma Halo CRD / stockage JSONB comme modèle de persistance.

**Conséquences :** Endpoints `/api/plugins` et `/api/plugins/{id}/assets/**` ; pas de couche « custom resource definition » au MVP. Détails de structure précisés dans ADR-007.

---

## ADR-004 — MongoDB d’abord, PostgreSQL derrière les mêmes ports

**Statut :** Accepté

**Contexte :** Besoin d’un store MVP rapide à itérer, tout en gardant une porte de sortie relationnelle.

**Décision :**

- MVP : adapter MongoDB (`adapters.out.persistence.mongo/`).
- Futur : adapter PostgreSQL/JPA derrière les **mêmes** ports repository.
- Interdit : fuite `ObjectId` / annotations Mongo dans le domaine.

**Conséquences :** Migrations Mongo versionnées côté adapter ; Flyway réservé au futur adapter Postgres ; bascule via profil/implémentation de port, sans réécrire les use cases.

---

## ADR-005 — Pas de moteur XML Axelor au MVP

**Statut :** Accepté

**Contexte :** L’exploration a évoqué Axelor (modèles XML, générateurs). Cela alourdit le MVP et contredit l’hexagone explicite + UI React contrôlée.

**Décision :** Aucun moteur XML Axelor, pas de génération de vues JSON Forms comme socle MVP. UI et API sont du code explicite (contrôleurs + React).

**Conséquences :** Prompts et doc excluent Axelor / Angular runtime / JSON Forms ; premier module = `plugin-rh` écrit à la main selon l’hexagone.

---

## ADR-006 — Shell UI inspiré d’erp-ang (Zard → shadcn)

**Statut :** Accepté

**Contexte :** Le projet `/Users/kabis/Documents/yidgri/erp-ang` fournit déjà un chrome ERP (sidebar 260px, breadcrumb, footer, tokens OKLCH neutral, page liste Members) basé sur ZardUI (équivalent Angular de shadcn) + Tailwind + Lucide.

**Décision :** Porter ce design dans `erp-ui` avec **shadcn/ui** + Tailwind + **lucide-react**. Copier les tokens (`tailwind.css` erp-ang) et le layout ; ajouter un écran Login JWT absent d’erp-ang. Ne pas dépendre du repo Angular au runtime.

**Conséquences :** Page Employés calquée sur `member-list` ; menu Core + slot `menu.main` ; primitives MVP Button, Input, Badge, Avatar, Breadcrumb (+ Table/Dropdown si besoin).

---

## ADR-007 — Structure plugin alignée Halo

**Statut :** Accepté

**Contexte :** La doc Halo ([structure](https://docs.halo.run/developer-guide/plugin/basics/structure), [manifest](https://docs.halo.run/developer-guide/plugin/basics/manifest)) décrit un plugin hybride `ui/` + Java + assets buildés + `plugin.yaml`, avec modes `deployment` / `development`.

**Décision :**

- Chaque plugin : `ui/` (Vite) → `src/main/resources/ui/` (`main.js`, `style.css`) + `plugin.yaml` obligatoire.
- Contrats partagés dans `erp-plugin-api` (équivalent *pluggable-suite*).
- Core : mode `deployment` (`./plugins`) et mode `development` (`fixed-plugin-path`).
- Extension points front : slots multi-instance (`dashboard.widgets`, `menu.main`).

**Conséquences :** Packaging et découverte UI standardisés ; pas d’adoption du modèle CRD / Extension resources Halo pour la persistance métier.

---

## ADR-008 — Communication inter-plugins (API + ExtensionPoint + SharedEvent)

**Statut :** Accepté

**Contexte :** Les contextes Spring des plugins sont isolés. Le soft-link Todo → collection Mongo `rh_employes` fonctionnait mais cassait l’encapsulation. Halo propose trois mécanismes : module `*-api`, extension points, événements partagés.

**Décision :**

1. **`plugin-<id>-api`** : contrats du fournisseur (DTO, `ExtensionPoint`, events). Consommateurs en `provided` ; Core dépend de l’API pour le classpath parent.
2. **`ExtensionGetter`** (Core) : résolution synchrone des beans d’extension exposés par les plugins démarrés.
3. **`@SharedEvent` + `SharedEventHub`** : relais d’événements Spring entre contextes enfants (et Core).
4. Interdit : dépendance Maven vers le JAR d’implémentation d’un autre plugin ; lecture directe des collections Mongo d’un autre module.

**Conséquences :** `plugin-rh-api` + migration Todo vers `EmployeDirectory` ; doc développeur [PLUGIN_INTERACTION.md](PLUGIN_INTERACTION.md). Pattern à répliquer pour tout nouveau partage inter-modules.
