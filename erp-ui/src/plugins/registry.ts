import type { ComponentType } from 'react';

export type ExtensionContribution = {
  id: string;
  pluginId: string;
  component: ComponentType;
  props?: Record<string, unknown>;
};

export type MenuContribution = {
  id: string;
  label: string;
  path: string;
  icon?: string;
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
      this.registerMenu({ id: contribution.id, label, path, icon });
      return;
    }
    this.notify();
  }

  registerMenu(item: MenuContribution) {
    this.menus = [...this.menus.filter((m) => m.id !== item.id), item];
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
