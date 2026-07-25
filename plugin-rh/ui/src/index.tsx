import type { ComponentType } from 'react';
import { EmployeListPage } from './views/EmployeListPage';
import { EmployeCountWidget } from './components/EmployeCountWidget';
import './styles.css';

export type ExtensionContribution = {
  id: string;
  pluginId: string;
  component: ComponentType;
  props?: Record<string, unknown>;
};

export type MenuContribution = {
  id: string;
  label: string;
  path?: string;
  icon?: string;
  permission?: string;
  children?: MenuContribution[];
};

export type PluginRuntimeApi = {
  registerExtension: (slot: string, contribution: ExtensionContribution) => void;
  registerMenu: (item: MenuContribution) => void;
  registerRoute: (path: string, component: ComponentType) => void;
  getAuthToken: () => string | null;
  getPermissions?: () => string[];
  apiBaseUrl: string;
};

declare global {
  interface Window {
    __PF4ERP__?: PluginRuntimeApi;
  }
}

export function activate(api: PluginRuntimeApi = window.__PF4ERP__!) {
  if (!api) {
    console.error('[plugin-rh] Plugin runtime API missing');
    return;
  }

  api.registerRoute('/rh/employes', EmployeListPage);

  // Menu simple (sans sous-menus) — visible si rh.employe.read
  api.registerMenu({
    id: 'rh.employes',
    label: 'Employees',
    path: '/rh/employes',
    icon: 'users',
    permission: 'rh.employe.read',
  });

  api.registerExtension('dashboard.widgets', {
    id: 'rh.employe-count',
    pluginId: 'rh',
    component: EmployeCountWidget,
  });
}

activate();
