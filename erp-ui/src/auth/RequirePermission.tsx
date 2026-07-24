import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthProvider';

type Props = {
  permission?: string;
  anyOf?: string[];
};

export function RequirePermission({ permission, anyOf }: Props) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-muted-foreground">
        Vérification des droits…
      </div>
    );
  }

  const perms = user?.permissions ?? [];
  const ok =
    (permission ? perms.includes(permission) : false) ||
    (anyOf ? anyOf.some((p) => perms.includes(p)) : false) ||
    (user?.roles.includes('ADMIN') ?? false);

  if (!ok) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
