import type { ComponentType } from 'react';
import { MyTasksPage } from './views/MyTasksPage';
import { ManageTodoPage } from './views/ManageTodoPage';
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
    console.error('[plugin-todo] Plugin runtime API missing');
    return;
  }

  api.registerRoute('/todo/mine', MyTasksPage);
  api.registerRoute('/todo/manage', ManageTodoPage);

  // Groupe + sous-menus : le shell filtre selon les permissions de l’utilisateur connecté
  api.registerMenu({
    id: 'todo',
    label: 'Todo',
    icon: 'list-todo',
    children: [
      {
        id: 'todo.mine',
        label: 'Mes tâches',
        path: '/todo/mine',
        icon: 'check-square',
        permission: 'todo.task.self',
      },
      {
        id: 'todo.manage',
        label: 'Gestion',
        path: '/todo/manage',
        icon: 'list-todo',
        permission: 'todo.project.manage',
      },
    ],
  });
}

activate();
