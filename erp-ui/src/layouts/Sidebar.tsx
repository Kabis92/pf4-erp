import { NavLink, useLocation } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import {
  CheckSquare,
  ChevronDown,
  ChevronsUpDown,
  HelpCircle,
  LayoutDashboard,
  ListTodo,
  LogOut,
  Puzzle,
  Settings,
  Shield,
  Users,
  UserCog,
} from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/auth/AuthProvider';
import { usePluginRegistry } from '@/plugins/loader';
import { filterMenusByPermission, type MenuContribution } from '@/plugins/registry';
import { cn } from '@/lib/utils';
import type { LucideIcon } from 'lucide-react';

const CORE_MENU = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
];

const SUPPORT_MENU = [
  { label: 'Paramètres', path: '/settings', icon: Settings },
  { label: 'Aide', path: '/help', icon: HelpCircle },
];

function menuIcon(icon?: string): LucideIcon {
  switch (icon) {
    case 'check-square':
      return CheckSquare;
    case 'list-todo':
      return ListTodo;
    case 'users':
      return Users;
    default:
      return Users;
  }
}

function pathMatches(pathname: string, path?: string) {
  if (!path) {
    return false;
  }
  return pathname === path || pathname.startsWith(`${path}/`);
}

function groupContainsActive(item: MenuContribution, pathname: string): boolean {
  if (pathMatches(pathname, item.path)) {
    return true;
  }
  return item.children?.some((child) => groupContainsActive(child, pathname)) ?? false;
}

export function Sidebar() {
  const { user, logout } = useAuth();
  const { menus } = usePluginRegistry();
  const location = useLocation();
  const perms = user?.permissions ?? [];
  const isAdmin = user?.roles.includes('ADMIN') ?? false;
  const visibleMenus = useMemo(
    () => filterMenusByPermission(menus, perms, isAdmin),
    [menus, perms, isAdmin],
  );
  const adminItems = [
    ...(perms.includes('core.user.read') || isAdmin
      ? [{ label: 'Utilisateurs', path: '/users', icon: UserCog }]
      : []),
    ...(perms.includes('core.role.read') || isAdmin
      ? [{ label: 'Rôles & accès', path: '/roles', icon: Shield }]
      : []),
    ...(perms.includes('core.plugin.admin') || isAdmin
      ? [{ label: 'Plugins', path: '/plugins', icon: Puzzle }]
      : []),
  ];
  const supportItems = [...adminItems, ...SUPPORT_MENU];
  const initials = (user?.displayName ?? 'U')
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setOpenGroups((prev) => {
      const next = { ...prev };
      for (const item of visibleMenus) {
        if (item.children?.length && groupContainsActive(item, location.pathname)) {
          next[item.id] = true;
        }
      }
      return next;
    });
  }, [visibleMenus, location.pathname]);

  function toggleGroup(id: string) {
    setOpenGroups((prev) => ({ ...prev, [id]: !prev[id] }));
  }

  function renderMenuItem(item: MenuContribution, depth = 0) {
    const Icon = menuIcon(item.icon);
    const hasChildren = Boolean(item.children?.length);

    if (hasChildren) {
      const open = openGroups[item.id] ?? groupContainsActive(item, location.pathname);
      return (
        <li key={item.id}>
          <button
            type="button"
            onClick={() => toggleGroup(item.id)}
            className={cn(
              'flex w-full items-center gap-2 rounded-lg px-2 py-2 text-sm text-sidebar-foreground hover:bg-sidebar-accent',
              depth > 0 && 'pl-4',
            )}
          >
            <Icon className="size-4 shrink-0" />
            <span className="min-w-0 flex-1 truncate text-left">{item.label}</span>
            <ChevronDown
              className={cn('size-4 shrink-0 transition-transform', open && 'rotate-180')}
            />
          </button>
          {open && (
            <ul className="mt-0.5 space-y-0.5 border-l border-border ml-4 pl-1">
              {item.children!.map((child) => renderMenuItem(child, depth + 1))}
            </ul>
          )}
        </li>
      );
    }

    if (!item.path) {
      return null;
    }

    return (
      <li key={item.id}>
        <NavLink
          to={item.path}
          className={({ isActive }) =>
            cn(
              'flex items-center gap-2 rounded-lg px-2 py-2 text-sm text-sidebar-foreground hover:bg-sidebar-accent',
              depth > 0 && 'pl-3',
              isActive && 'bg-sidebar-accent font-medium',
            )
          }
        >
          <Icon className="size-4 shrink-0" />
          {item.label}
        </NavLink>
      </li>
    );
  }

  return (
    <aside className="flex h-full w-[260px] shrink-0 flex-col border-r border-border bg-sidebar">
      <div className="flex items-center gap-3 border-b border-border px-5 py-4">
        <div className="flex size-9 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground">
          E
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-sidebar-foreground">ERP Suite</p>
          <p className="truncate text-xs text-muted-foreground">Workspace</p>
        </div>
        <Button variant="ghost" size="icon" className="size-8 shrink-0">
          <ChevronsUpDown className="size-4" />
        </Button>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-5">
        <p className="mb-2 px-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">Menu</p>
        <ul className="space-y-1">
          {CORE_MENU.map((item) => (
            <li key={item.path}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-2 rounded-lg px-2 py-2 text-sm text-sidebar-foreground hover:bg-sidebar-accent',
                    isActive && 'bg-sidebar-accent font-medium',
                  )
                }
              >
                <item.icon className="size-4" />
                {item.label}
              </NavLink>
            </li>
          ))}
          {visibleMenus.map((item) => renderMenuItem(item))}
        </ul>

        <div className="my-4 border-t border-border" />
        <p className="mb-2 px-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">Support</p>
        <ul className="space-y-1">
          {supportItems.map((item) => (
            <li key={item.path}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-2 rounded-lg px-2 py-2 text-sm text-sidebar-foreground hover:bg-sidebar-accent',
                    isActive && 'bg-sidebar-accent font-medium',
                  )
                }
              >
                <item.icon className="size-4" />
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="border-t border-border p-3">
        <div className="flex items-center gap-3 rounded-lg px-2 py-2">
          <Avatar fallback={initials || 'U'} className="size-8" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium">{user?.displayName}</p>
            <p className="truncate text-xs text-muted-foreground">{user?.username}@erp.local</p>
          </div>
          <Button variant="ghost" size="icon" className="size-8 shrink-0" aria-label="Déconnexion" onClick={logout}>
            <LogOut className="size-4" />
          </Button>
        </div>
      </div>
    </aside>
  );
}
