import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthProvider';

export function RequireAdmin() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
        Vérification des droits…
      </div>
    );
  }

  if (!user?.roles.includes('ADMIN')) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
