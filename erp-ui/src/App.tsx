import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from '@/auth/RequireAuth';
import { RequirePermission } from '@/auth/RequirePermission';
import { MainLayout } from '@/layouts/MainLayout';
import { LoginPage } from '@/pages/LoginPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { PlaceholderPage } from '@/pages/PlaceholderPage';
import { PluginsAdminPage } from '@/pages/PluginsAdminPage';
import { UsersAdminPage } from '@/pages/UsersAdminPage';
import { RolesAdminPage } from '@/pages/RolesAdminPage';
import { loadPlugins, usePluginRegistry } from '@/plugins/loader';
import { useAuth } from '@/auth/AuthProvider';

function AuthenticatedApp() {
  const { user } = useAuth();
  const { routes } = usePluginRegistry();
  const [pluginsReady, setPluginsReady] = useState(false);
  const [pluginError, setPluginError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      return;
    }
    let cancelled = false;
    loadPlugins()
      .then(() => {
        if (!cancelled) {
          setPluginsReady(true);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setPluginError(err.message);
          setPluginsReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [user]);

  if (!pluginsReady) {
    return (
      <div className="flex h-dvh items-center justify-center text-sm text-muted-foreground">
        Chargement des plugins…
      </div>
    );
  }

  return (
    <>
      {pluginError && (
        <div className="border-b border-destructive/30 bg-destructive/10 px-4 py-2 text-center text-xs text-destructive">
          {pluginError}
        </div>
      )}
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/settings" element={<PlaceholderPage title="Paramètres" />} />
          <Route path="/help" element={<PlaceholderPage title="Aide" />} />
          <Route element={<RequirePermission permission="core.user.read" />}>
            <Route path="/users" element={<UsersAdminPage />} />
          </Route>
          <Route element={<RequirePermission permission="core.role.read" />}>
            <Route path="/roles" element={<RolesAdminPage />} />
          </Route>
          <Route element={<RequirePermission permission="core.plugin.admin" />}>
            <Route path="/plugins" element={<PluginsAdminPage />} />
          </Route>
          {routes.map(({ path, component: Comp }) => (
            <Route key={path} path={path} element={<Comp />} />
          ))}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </>
  );
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/*" element={<AuthenticatedApp />} />
      </Route>
    </Routes>
  );
}
