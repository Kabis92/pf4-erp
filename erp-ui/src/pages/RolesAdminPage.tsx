import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Loader2, Plus, RefreshCw, Shield, Trash2 } from 'lucide-react';
import { adminFetch } from '@/lib/adminApi';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAuth } from '@/auth/AuthProvider';

type AdminRole = {
  id: string;
  code: string;
  label: string;
  description: string;
  permissionCodes: string[];
  system: boolean;
};

type AdminPermission = {
  code: string;
  label: string;
  pluginId: string | null;
  active: boolean;
  core: boolean;
};

export function RolesAdminPage() {
  const { user: currentUser } = useAuth();
  const canWrite = currentUser?.permissions.includes('core.role.write') || currentUser?.roles.includes('ADMIN');
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [permissions, setPermissions] = useState<AdminPermission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ code: '', label: '', description: '' });
  const [editForm, setEditForm] = useState({
    label: '',
    description: '',
    permissionCodes: [] as string[],
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [r, p] = await Promise.all([
        adminFetch<AdminRole[]>('/api/admin/roles'),
        adminFetch<AdminPermission[]>('/api/admin/permissions'),
      ]);
      setRoles(r);
      setPermissions(p);
      if (!selectedId && r.length > 0) {
        setSelectedId(r[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selected = roles.find((r) => r.id === selectedId) ?? null;

  useEffect(() => {
    if (!selected) {
      return;
    }
    setEditForm({
      label: selected.label,
      description: selected.description,
      permissionCodes: [...selected.permissionCodes],
    });
  }, [selected]);

  const groupedPermissions = useMemo(() => {
    const groups = new Map<string, AdminPermission[]>();
    for (const perm of permissions) {
      const key = perm.core ? 'Core' : perm.pluginId || 'Autres';
      const list = groups.get(key) ?? [];
      list.push(perm);
      groups.set(key, list);
    }
    return [...groups.entries()];
  }, [permissions]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      const created = await adminFetch<AdminRole>('/api/admin/roles', {
        method: 'POST',
        body: JSON.stringify({ ...createForm, permissionCodes: [] }),
      });
      setSuccess(`Rôle « ${created.code} » créé.`);
      setCreateForm({ code: '', label: '', description: '' });
      setShowCreate(false);
      await load();
      setSelectedId(created.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Création impossible');
    }
  }

  async function onSave() {
    if (!selected) {
      return;
    }
    setBusyId(selected.id);
    setError(null);
    setSuccess(null);
    try {
      await adminFetch(`/api/admin/roles/${selected.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          label: editForm.label,
          description: editForm.description,
          permissionCodes: selected.code === 'ADMIN' ? [] : editForm.permissionCodes,
        }),
      });
      setSuccess(`Rôle « ${selected.code} » enregistré. Les utilisateurs devront se reconnecter pour appliquer les nouveaux droits.`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enregistrement impossible');
    } finally {
      setBusyId(null);
    }
  }

  async function onDelete(role: AdminRole) {
    if (!window.confirm(`Supprimer le rôle « ${role.code} » ?`)) {
      return;
    }
    setBusyId(role.id);
    setError(null);
    setSuccess(null);
    try {
      await adminFetch(`/api/admin/roles/${role.id}`, { method: 'DELETE' });
      setSuccess(`Rôle « ${role.code} » supprimé.`);
      if (selectedId === role.id) {
        setSelectedId(null);
      }
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setBusyId(null);
    }
  }

  function togglePerm(code: string) {
    setEditForm((f) => ({
      ...f,
      permissionCodes: f.permissionCodes.includes(code)
        ? f.permissionCodes.filter((c) => c !== code)
        : [...f.permissionCodes, code],
    }));
  }

  return (
    <div className="flex h-full flex-col overflow-hidden p-6">
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold">Rôles & accès</h1>
          <p className="text-sm text-muted-foreground">
            Assignez les permissions du Core et des plugins actifs aux rôles.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => void load()} disabled={loading}>
            <RefreshCw className={`size-4 ${loading ? 'animate-spin' : ''}`} />
            Actualiser
          </Button>
          {canWrite && (
            <Button size="sm" onClick={() => setShowCreate((v) => !v)}>
              <Plus className="size-4" />
              Nouveau rôle
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
        <form onSubmit={onCreate} className="mb-4 grid gap-3 rounded-xl border border-border bg-card p-4 sm:grid-cols-3">
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Code</label>
            <Input
              value={createForm.code}
              onChange={(e) => setCreateForm((f) => ({ ...f, code: e.target.value }))}
              placeholder="RH_MANAGER"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Libellé</label>
            <Input
              value={createForm.label}
              onChange={(e) => setCreateForm((f) => ({ ...f, label: e.target.value }))}
              placeholder="Manager RH"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Description</label>
            <Input
              value={createForm.description}
              onChange={(e) => setCreateForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <div className="flex gap-2 sm:col-span-3">
            <Button type="submit" size="sm">
              Créer
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => setShowCreate(false)}>
              Annuler
            </Button>
          </div>
        </form>
      )}

      <div className="grid min-h-0 flex-1 gap-4 lg:grid-cols-[240px_1fr]">
        <div className="overflow-y-auto rounded-xl border border-border bg-card">
          {loading && (
            <div className="flex items-center justify-center gap-2 px-4 py-10 text-sm text-muted-foreground">
              <Loader2 className="size-4 animate-spin" />
            </div>
          )}
          {!loading &&
            roles.map((role) => (
              <button
                key={role.id}
                type="button"
                onClick={() => setSelectedId(role.id)}
                className={`flex w-full items-start gap-2 border-b border-border px-4 py-3 text-left last:border-b-0 hover:bg-muted/40 ${
                  selectedId === role.id ? 'bg-muted/50' : ''
                }`}
              >
                <Shield className="mt-0.5 size-4 shrink-0" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{role.label}</p>
                  <p className="text-xs text-muted-foreground">{role.code}</p>
                </div>
              </button>
            ))}
        </div>

        <div className="overflow-y-auto rounded-xl border border-border bg-card p-5">
          {!selected && !loading && (
            <p className="text-sm text-muted-foreground">Sélectionnez un rôle.</p>
          )}
          {selected && (
            <>
              <div className="mb-4 flex flex-wrap items-center gap-2">
                <h2 className="text-lg font-semibold">{selected.label}</h2>
                <Badge variant="outline">{selected.code}</Badge>
                {selected.system && <Badge variant="secondary">Système</Badge>}
                {selected.code === 'ADMIN' && (
                  <Badge variant="secondary">Toutes les permissions actives</Badge>
                )}
              </div>

              <div className="mb-4 grid gap-3 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-xs text-muted-foreground">Libellé</label>
                  <Input
                    value={editForm.label}
                    disabled={!canWrite}
                    onChange={(e) => setEditForm((f) => ({ ...f, label: e.target.value }))}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-muted-foreground">Description</label>
                  <Input
                    value={editForm.description}
                    disabled={!canWrite}
                    onChange={(e) => setEditForm((f) => ({ ...f, description: e.target.value }))}
                  />
                </div>
              </div>

              {selected.code !== 'ADMIN' && (
                <div className="space-y-4">
                  <p className="text-sm font-medium">Permissions</p>
                  {groupedPermissions.map(([group, perms]) => (
                    <div key={group}>
                      <p className="mb-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        {group}
                      </p>
                      <ul className="space-y-2">
                        {perms.map((perm) => (
                          <li key={perm.code} className="flex items-start gap-2 text-sm">
                            <input
                              type="checkbox"
                              className="mt-1"
                              disabled={!canWrite || !perm.active}
                              checked={editForm.permissionCodes.includes(perm.code)}
                              onChange={() => togglePerm(perm.code)}
                            />
                            <div>
                              <p className={!perm.active ? 'text-muted-foreground' : ''}>
                                {perm.label}
                                {!perm.active && (
                                  <span className="ml-2 text-xs">(plugin inactif)</span>
                                )}
                              </p>
                              <p className="text-xs text-muted-foreground">{perm.code}</p>
                            </div>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              )}

              {canWrite && (
                <div className="mt-6 flex flex-wrap gap-2">
                  <Button size="sm" disabled={busyId === selected.id} onClick={() => void onSave()}>
                    {busyId === selected.id ? <Loader2 className="size-4 animate-spin" /> : null}
                    Enregistrer
                  </Button>
                  {!selected.system && (
                    <Button
                      variant="outline"
                      size="sm"
                      className="text-destructive hover:text-destructive"
                      disabled={busyId === selected.id}
                      onClick={() => void onDelete(selected)}
                    >
                      <Trash2 className="size-4" />
                      Supprimer
                    </Button>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
