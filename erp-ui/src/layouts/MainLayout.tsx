import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { Footer } from './Footer';
import type { Crumb } from '@/components/ui/breadcrumb';

function crumbsFor(pathname: string): Crumb[] {
  if (pathname.startsWith('/rh/employes')) {
    return [
      { label: 'Dashboard', to: '/dashboard' },
      { label: 'RH' },
      { label: 'Employés' },
    ];
  }
  if (pathname.startsWith('/plugins')) {
    return [{ label: 'Support' }, { label: 'Plugins' }];
  }
  if (pathname.startsWith('/dashboard')) {
    return [{ label: 'Dashboard' }];
  }
  return [{ label: 'ERP Suite' }];
}

export function MainLayout() {
  const location = useLocation();
  return (
    <div className="flex h-dvh overflow-hidden bg-background">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Header crumbs={crumbsFor(location.pathname)} />
        <main className="min-h-0 flex-1 overflow-hidden">
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
  );
}
