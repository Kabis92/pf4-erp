import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as ReactJsxRuntime from 'react/jsx-runtime';
import * as ReactRouterDOM from 'react-router-dom';
import { getStoredUser, getToken } from '@/lib/auth';
import { pluginRegistry, type PluginDefinition } from './registry';
import { useSyncExternalStore } from 'react';

export type PluginRuntimeApi = {
  registerExtension: typeof pluginRegistry.registerExtension;
  registerMenu: typeof pluginRegistry.registerMenu;
  registerRoute: typeof pluginRegistry.registerRoute;
  getAuthToken: () => string | null;
  getPermissions: () => string[];
  apiBaseUrl: string;
};

declare global {
  interface Window {
    __PF4ERP__?: PluginRuntimeApi;
    React: typeof React;
    ReactDOM: typeof ReactDOM;
    ReactJsxRuntime: typeof ReactJsxRuntime;
    ReactRouterDOM: typeof ReactRouterDOM;
  }
}

function exposeSharedDeps() {
  window.React = React;
  window.ReactDOM = ReactDOM;
  window.ReactJsxRuntime = ReactJsxRuntime;
  window.ReactRouterDOM = ReactRouterDOM;
}

function ensureRuntimeApi() {
  if (!window.__PF4ERP__) {
    window.__PF4ERP__ = {
      registerExtension: (slot, contribution) => pluginRegistry.registerExtension(slot, contribution),
      registerMenu: (item) => pluginRegistry.registerMenu(item),
      registerRoute: (path, component) => pluginRegistry.registerRoute(path, component),
      getAuthToken: () => getToken(),
      getPermissions: () => getStoredUser()?.permissions ?? [],
      apiBaseUrl: '',
    };
  }
  return window.__PF4ERP__;
}

function loadScript(src: string) {
  return new Promise<void>((resolve, reject) => {
    // Drop previous tag for same plugin asset path (ignore query) so updates reload
    const base = src.split('?')[0];
    document
      .querySelectorAll<HTMLScriptElement>('script[data-plugin-src]')
      .forEach((el) => {
        const prev = el.dataset.pluginSrc ?? '';
        if (prev.split('?')[0] === base) {
          el.remove();
        }
      });
    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    script.dataset.pluginSrc = src;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error(`Failed to load plugin script: ${src}`));
    document.head.appendChild(script);
  });
}

function loadStylesheet(href: string) {
  if (document.querySelector(`link[data-plugin-href="${href}"]`)) {
    return;
  }
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = href;
  link.dataset.pluginHref = href;
  document.head.appendChild(link);
}

export async function loadPlugins(): Promise<PluginDefinition[]> {
  exposeSharedDeps();
  ensureRuntimeApi();
  pluginRegistry.clear();

  const token = getToken();
  const res = await fetch('/api/plugins', {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`Impossible de charger les plugins (${res.status})`);
  }
  const plugins = (await res.json()) as PluginDefinition[];

  for (const plugin of plugins) {
    if (!plugin.started || !plugin.entry) {
      continue;
    }
    try {
      const entryPath = plugin.entry.split('?')[0];
      const styleFromEntry = entryPath.replace(/main\.js$/, 'style.css');
      if (styleFromEntry !== entryPath) {
        loadStylesheet(styleFromEntry);
      }
      const bust = plugin.entry.includes('?')
        ? `${plugin.entry}&_=${Date.now()}`
        : `${plugin.entry}?_=${Date.now()}`;
      await loadScript(bust);
    } catch (err) {
      console.error(`[PluginLoader] failed to load ${plugin.id}`, err);
    }
  }

  return plugins;
}

export function usePluginRegistry() {
  return useSyncExternalStore(
    (cb) => pluginRegistry.subscribe(cb),
    () => pluginRegistry.getSnapshot(),
  );
}
