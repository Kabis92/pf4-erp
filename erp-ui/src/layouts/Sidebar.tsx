import { NavLink } from 'react-router-dom';
import {
  ChevronsUpDown,
  HelpCircle,
  LayoutDashboard,
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
import { cn } from '@/lib/utils';

const CORE_MENU = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
];

const SUPPORT_MENU = [
  { label: 'Paramètres', path: '/settings', icon: Settings },
  { label: 'Aide', path: '/help', icon: HelpCircle },
];

export function Sidebar() {
  const { user, logout } = useAuth();
  const { menus } = usePluginRegistry();
  const perms = user?.permissions ?? [];
  const isAdmin = user?.roles.includes('ADMIN') ?? false;
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
          {menus.map((item) => (
            <li key={item.id}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-2 rounded-lg px-2 py-2 text-sm text-sidebar-foreground hover:bg-sidebar-accent',
                    isActive && 'bg-sidebar-accent font-medium',
                  )
                }
              >
                <Users className="size-4" />
                {item.label}
              </NavLink>
            </li>
          ))}
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
