import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Loader2, Plus, RefreshCw, Trash2, UserPlus } from 'lucide-react';
import { adminFetch } from '@/lib/adminApi';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/auth/AuthProvider';

type AdminUser = {
  id: string;
  username: string;
  displayName: string;
  roleCodes: string[];
  enabled: boolean;
};

type AdminRole = {
  id: string;
  code: string;
  label: string;
};

const emptyForm = {
  username: '',
  displayName: '',
  password: '',
  roleCodes: ['USER'] as string[],
  enabled: true,
};

export function UsersAdminPage() {
  const { user: currentUser } = useAuth();
  const canWrite = currentUser?.permissions.includes('core.user.write') || currentUser?.roles.includes('ADMIN');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState({
    displayName: '',
    password: '',
    roleCodes: [] as string[],
    enabled: true,
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [u, r] = await Promise.all([
        adminFetch<AdminUser[]>('/api/admin/users'),
        adminFetch<AdminRole[]>('/api/admin/roles'),
      ]);
      setUsers(u);
      setRoles(r);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      await adminFetch('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify(form),
      });
      setSuccess(`Utilisateur « ${form.username} » créé.`);
      setForm(emptyForm);
      setShowCreate(false);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Création impossible');
    }
  }

  async function onSaveEdit(id: string) {
    setBusyId(id);
    setError(null);
    setSuccess(null);
    try {
      await adminFetch(`/api/admin/users/${id}`, {
        method: 'PUT',
        body: JSON.stringify({
          displayName: editForm.displayName,
          password: editForm.password || undefined,
          roleCodes: editForm.roleCodes,
          enabled: editForm.enabled,
        }),
      });
      setSuccess('Utilisateur mis à jour.');
      setEditId(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Mise à jour impossible');
    } finally {
      setBusyId(null);
    }
  }

  async function onDelete(user: AdminUser) {
    if (!window.confirm(`Supprimer « ${user.username} » ?`)) {
      return;
    }
    setBusyId(user.id);
    setError(null);
    setSuccess(null);
    try {
      await adminFetch(`/api/admin/users/${user.id}`, { method: 'DELETE' });
      setSuccess(`Utilisateur « ${user.username} » supprimé.`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setBusyId(null);
    }
  }

  function toggleRole(codes: string[], code: string) {
    return codes.includes(code) ? codes.filter((c) => c !== code) : [...codes, code];
  }

  return (
    <div className="flex h-full flex-col overflow-hidden p-6">
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold">Utilisateurs</h1>
          <p className="text-sm text-muted-foreground">
            Créer et gérer les comptes. Les droits viennent des rôles assignés.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => void load()} disabled={loading}>
            <RefreshCw className={`size-4 ${loading ? 'animate-spin' : ''}`} />
            Actualiser
          </Button>
          {canWrite && (
            <Button size="sm" onClick={() => setShowCreate((v) => !v)}>
              <UserPlus className="size-4" />
              Nouvel utilisateur
            </Button>
          )}
        </div>
      </div>

      {success && (
        <div className="mb-4 rounded-md border border-border bg-muted/40 px-3 py-2 text-sm">{success}</div>
      )}
      {error && (
        <div className="mb-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </div>
      )}

      {showCreate && canWrite && (
        <form onSubmit={onCreate} className="mb-4 grid gap-3 rounded-xl border border-border bg-card p-4 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Identifiant</label>
            <Input
              value={form.username}
              onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
              required
              autoComplete="off"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Nom affiché</label>
            <Input
              value={form.displayName}
              onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Mot de passe</label>
            <Input
              type="password"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              required
              autoComplete="new-password"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Rôles</label>
            <div className="flex flex-wrap gap-2 pt-1">
              {roles.map((role) => (
                <label key={role.code} className="flex items-center gap-1.5 text-sm">
                  <input
                    type="checkbox"
                    checked={form.roleCodes.includes(role.code)}
                    onChange={() =>
                      setForm((f) => ({ ...f, roleCodes: toggleRole(f.roleCodes, role.code) }))
                    }
                  />
                  {role.label}
                </label>
              ))}
            </div>
          </div>
          <div className="flex items-end gap-2 sm:col-span-2">
            <Button type="submit" size="sm">
              <Plus className="size-4" />
              Créer
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => setShowCreate(false)}>
              Annuler
            </Button>
          </div>
        </form>
      )}

      <div className="min-h-0 flex-1 overflow-y-auto rounded-xl border border-border bg-card shadow-sm">
        {loading && (
          <div className="flex items-center justify-center gap-2 px-6 py-16 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" />
            Chargement…
          </div>
        )}
        {!loading &&
          users.map((user) => {
            const editing = editId === user.id;
            return (
              <div key={user.id} className="border-b border-border px-5 py-4 last:border-b-0">
                {!editing && (
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-semibold">{user.displayName}</p>
                        <Badge variant={user.enabled ? 'secondary' : 'outline'}>
                          {user.enabled ? 'Actif' : 'Désactivé'}
                        </Badge>
                        <span className="text-xs text-muted-foreground">@{user.username}</span>
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">
                        Rôles : {user.roleCodes.join(', ') || '—'}
                      </p>
                    </div>
                    {canWrite && (
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={busyId === user.id}
                          onClick={() => {
                            setEditId(user.id);
                            setEditForm({
                              displayName: user.displayName,
                              password: '',
                              roleCodes: [...user.roleCodes],
                              enabled: user.enabled,
                            });
                          }}
                        >
                          Modifier
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="text-destructive hover:text-destructive"
                          disabled={busyId === user.id}
                          onClick={() => void onDelete(user)}
                        >
                          {busyId === user.id ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
                          Supprimer
                        </Button>
                      </div>
                    )}
                  </div>
                )}
                {editing && (
                  <div className="grid gap-3 sm:grid-cols-2">
                    <div>
                      <label className="mb-1 block text-xs text-muted-foreground">Nom affiché</label>
                      <Input
                        value={editForm.displayName}
                        onChange={(e) => setEditForm((f) => ({ ...f, displayName: e.target.value }))}
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs text-muted-foreground">Nouveau mot de passe</label>
                      <Input
                        type="password"
                        placeholder="Laisser vide pour ne pas changer"
                        value={editForm.password}
                        onChange={(e) => setEditForm((f) => ({ ...f, password: e.target.value }))}
                        autoComplete="new-password"
                      />
                    </div>
                    <div className="sm:col-span-2">
                      <label className="mb-1 block text-xs text-muted-foreground">Rôles</label>
                      <div className="flex flex-wrap gap-2">
                        {roles.map((role) => (
                          <label key={role.code} className="flex items-center gap-1.5 text-sm">
                            <input
                              type="checkbox"
                              checked={editForm.roleCodes.includes(role.code)}
                              onChange={() =>
                                setEditForm((f) => ({
                                  ...f,
                                  roleCodes: toggleRole(f.roleCodes, role.code),
                                }))
                              }
                            />
                            {role.label}
                          </label>
                        ))}
                      </div>
                    </div>
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={editForm.enabled}
                        onChange={(e) => setEditForm((f) => ({ ...f, enabled: e.target.checked }))}
                      />
                      Compte actif
                    </label>
                    <div className="flex items-end gap-2 sm:col-span-2">
                      <Button size="sm" disabled={busyId === user.id} onClick={() => void onSaveEdit(user.id)}>
                        {busyId === user.id ? <Loader2 className="size-4 animate-spin" /> : null}
                        Enregistrer
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => setEditId(null)}>
                        Annuler
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
      </div>
    </div>
  );
}
