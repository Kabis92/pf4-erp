import type { ComponentType } from 'react';

export type ExtensionContribution = {
  id: string;
  pluginId: string;
  component: ComponentType;
  props?: Record<string, unknown>;
};

/** Entrée de menu plugin : feuille (path) ou groupe (children). */
export type MenuContribution = {
  id: string;
  label: string;
  /** Requis pour une feuille ; omis pour un groupe avec children. */
  path?: string;
  icon?: string;
  /** Si défini, visible seulement si l’utilisateur a cette permission (ou ADMIN). */
  permission?: string;
  children?: MenuContribution[];
};

export type PluginDefinition = {
  id: string;
  version: string;
  displayName: string;
  description: string;
  entry: string;
  uiSlots: string[];
  started: boolean;
};

export type PluginRoute = {
  path: string;
  component: ComponentType;
};

export type PluginRegistrySnapshot = {
  menus: MenuContribution[];
  routes: PluginRoute[];
  widgets: ExtensionContribution[];
};

type Listener = () => void;

const EMPTY_EXTENSIONS: ExtensionContribution[] = [];

class PluginRegistry {
  private extensions = new Map<string, ExtensionContribution[]>();
  private menus: MenuContribution[] = [];
  private routes = new Map<string, ComponentType>();
  private listeners = new Set<Listener>();
  private snapshot: PluginRegistrySnapshot = {
    menus: this.menus,
    routes: [],
    widgets: EMPTY_EXTENSIONS,
  };

  subscribe(listener: Listener) {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify() {
    this.snapshot = {
      menus: this.menus,
      routes: Array.from(this.routes.entries()).map(([path, component]) => ({ path, component })),
      widgets: this.extensions.get('dashboard.widgets') ?? EMPTY_EXTENSIONS,
    };
    this.listeners.forEach((l) => l());
  }

  registerExtension(slot: string, contribution: ExtensionContribution) {
    const list = this.extensions.get(slot) ?? [];
    const next = [...list.filter((c) => c.id !== contribution.id), contribution];
    this.extensions.set(slot, next);
    if (slot === 'menu.main' && contribution.props) {
      const label = String(contribution.props.label ?? contribution.id);
      const path = String(contribution.props.path ?? '/');
      const icon = contribution.props.icon ? String(contribution.props.icon) : undefined;
      const permission = contribution.props.permission
        ? String(contribution.props.permission)
        : undefined;
      this.registerMenu({ id: contribution.id, label, path, icon, permission });
      return;
    }
    this.notify();
  }

  registerMenu(item: MenuContribution) {
    this.menus = [...this.menus.filter((m) => m.id !== item.id), item];
    this.notify();
  }

  /** Remet à zéro menus/routes/extensions avant un (re)chargement de plugins. */
  clear() {
    this.extensions = new Map();
    this.menus = [];
    this.routes = new Map();
    this.notify();
  }

  registerRoute(path: string, component: ComponentType) {
    this.routes.set(path, component);
    this.notify();
  }

  getExtensions(slot: string) {
    return this.extensions.get(slot) ?? EMPTY_EXTENSIONS;
  }

  getSnapshot(): PluginRegistrySnapshot {
    return this.snapshot;
  }
}

export const pluginRegistry = new PluginRegistry();

/** Filtre récursif des menus selon les permissions de l’utilisateur connecté. */
export function filterMenusByPermission(
  menus: MenuContribution[],
  permissions: string[],
  isAdmin: boolean,
): MenuContribution[] {
  const can = (permission?: string) =>
    !permission || isAdmin || permissions.includes(permission);

  return menus
    .map((item) => {
      if (item.children && item.children.length > 0) {
        const children = filterMenusByPermission(item.children, permissions, isAdmin);
        if (children.length === 0) {
          return null;
        }
        // Groupe visible si au moins un enfant l’est (et si le groupe a une permission, elle doit passer)
        if (!can(item.permission)) {
          return null;
        }
        return { ...item, children };
      }
      if (!item.path || !can(item.permission)) {
        return null;
      }
      return item;
    })
    .filter((item): item is MenuContribution => item != null);
}
