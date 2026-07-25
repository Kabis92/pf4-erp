import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  ArrowDownUp,
  CalendarDays,
  Download,
  Eye,
  Pencil,
  Plus,
  Search,
  Trash2,
  UserCheck,
  UserMinus,
  Users,
} from 'lucide-react';
import {
  createEmploye,
  deleteEmploye,
  formatJoiningDate,
  listEmployes,
  setEmployeAccess,
  statutLabel,
  updateEmploye,
  type Employe,
  type EmployeInput,
  type EmployeStatut,
} from '../api/employes';
import { AccessEnableDialog } from '../components/AccessEnableDialog';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmployeFormDialog } from '../components/EmployeFormDialog';

type FormState =
  | { open: false }
  | { open: true; mode: 'create' }
  | { open: true; mode: 'edit'; employe: Employe };

type ViewState = Employe | null;

function initials(name: string) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

function normalize(e: Employe): Employe {
  const statut =
    e.statut === 'CONGE'
      ? 'CONGE'
      : e.statut === 'INACTIF' || e.statut === ('ONBOARDING' as EmployeStatut) || e.statut === ('SORTIE' as EmployeStatut)
        ? 'INACTIF'
        : 'ACTIF';
  return {
    ...e,
    telephone: e.telephone ?? null,
    statut,
    hasAccess: e.hasAccess ?? Boolean(e.userId),
  };
}

function statutBadgeClass(statut: EmployeStatut) {
  switch (statut) {
    case 'ACTIF':
      return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300';
    case 'CONGE':
      return 'bg-orange-500/15 text-orange-700 dark:text-orange-300';
    case 'INACTIF':
      return 'bg-muted text-muted-foreground';
    default:
      return 'bg-secondary text-secondary-foreground';
  }
}

export function EmployeListPage() {
  const [employes, setEmployes] = useState<Employe[]>([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | EmployeStatut>('ALL');
  const [accessFilter, setAccessFilter] = useState<'ALL' | 'ENABLED' | 'DISABLED'>('ALL');
  const [sortAsc, setSortAsc] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>({ open: false });
  const [view, setView] = useState<ViewState>(null);
  const [toDelete, setToDelete] = useState<Employe | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [accessTarget, setAccessTarget] = useState<Employe | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setEmployes((await listEmployes()).map(normalize));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Load failed');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const stats = useMemo(() => {
    const total = employes.length;
    const actifs = employes.filter((e) => e.statut === 'ACTIF').length;
    const conge = employes.filter((e) => e.statut === 'CONGE').length;
    const inactifs = employes.filter((e) => e.statut === 'INACTIF').length;
    return { total, actifs, conge, inactifs };
  }, [employes]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    let rows = employes;
    if (statusFilter !== 'ALL') {
      rows = rows.filter((e) => e.statut === statusFilter);
    }
    if (accessFilter === 'ENABLED') {
      rows = rows.filter((e) => e.hasAccess);
    } else if (accessFilter === 'DISABLED') {
      rows = rows.filter((e) => !e.hasAccess);
    }
    if (q) {
      rows = rows.filter(
        (e) =>
          e.nom.toLowerCase().includes(q) ||
          e.email.toLowerCase().includes(q) ||
          (e.telephone ?? '').toLowerCase().includes(q) ||
          e.poste.toLowerCase().includes(q),
      );
    }
    return [...rows].sort((a, b) => {
      const cmp = a.nom.localeCompare(b.nom, 'en', { sensitivity: 'base' });
      return sortAsc ? cmp : -cmp;
    });
  }, [employes, search, statusFilter, accessFilter, sortAsc]);

  async function handleSave(input: EmployeInput) {
    if (form.open && form.mode === 'edit') {
      await updateEmploye(form.employe.id, input);
    } else {
      await createEmploye(input);
    }
    setActionError(null);
    await load();
  }

  async function handleDelete() {
    if (!toDelete) return;
    setDeleting(true);
    setActionError(null);
    try {
      await deleteEmploye(toDelete.id);
      setToDelete(null);
      await load();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Delete failed');
    } finally {
      setDeleting(false);
    }
  }

  async function handleToggleAccess(employe: Employe, enabled: boolean) {
    setActionError(null);
    if (enabled && !employe.userId) {
      setAccessTarget(employe);
      return;
    }
    setTogglingId(employe.id);
    try {
      await setEmployeAccess(employe.id, enabled, employe.userId);
      await load();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Access update failed');
    } finally {
      setTogglingId(null);
    }
  }

  function exportCsv() {
    const header = ['Name', 'Position', 'Email', 'Phone', 'Joining Date', 'Status', 'System Access'];
    const lines = filtered.map((e) =>
      [
        e.nom,
        e.poste,
        e.email,
        e.telephone ?? '',
        e.dateEmbauche,
        statutLabel(e.statut),
        e.hasAccess ? 'Enabled' : 'Disabled',
      ]
        .map((v) => `"${String(v).replaceAll('"', '""')}"`)
        .join(','),
    );
    const blob = new Blob([[header.join(','), ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'employees.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="flex h-full flex-col overflow-hidden bg-background">
      <div className="shrink-0 space-y-5 px-6 pt-6 pb-2">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Employees</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Manage your {stats.total} employee{stats.total === 1 ? '' : 's'}.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setForm({ open: true, mode: 'create' })}
            className="inline-flex h-10 shrink-0 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90"
          >
            <Plus className="size-4" />
            Add Employee
          </button>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Total Employees"
            value={stats.total}
            icon={<Users className="size-4 text-sky-600" />}
            iconWrap="bg-sky-500/10"
          />
          <StatCard
            label="Active Employees"
            value={stats.actifs}
            icon={<UserCheck className="size-4 text-emerald-600" />}
            iconWrap="bg-emerald-500/10"
          />
          <StatCard
            label="On Leave"
            value={stats.conge}
            icon={<CalendarDays className="size-4 text-orange-600" />}
            iconWrap="bg-orange-500/10"
          />
          <StatCard
            label="Inactive Employees"
            value={stats.inactifs}
            icon={<UserMinus className="size-4 text-rose-600" />}
            iconWrap="bg-rose-500/10"
          />
        </div>

        <div className="flex flex-col gap-3 rounded-xl border border-border bg-card p-3 shadow-sm lg:flex-row lg:items-center">
          <div className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="search"
              placeholder="Search employees by name, email or phone number."
              className="flex h-10 w-full rounded-lg border border-input bg-transparent py-1 pr-3 pl-9 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as typeof statusFilter)}
            className="h-10 rounded-lg border border-input bg-transparent px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <option value="ALL">Select status</option>
            <option value="ACTIF">Active</option>
            <option value="CONGE">On Leave</option>
            <option value="INACTIF">Inactive</option>
          </select>
          <select
            value={accessFilter}
            onChange={(e) => setAccessFilter(e.target.value as typeof accessFilter)}
            className="h-10 rounded-lg border border-input bg-transparent px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <option value="ALL">Select Access</option>
            <option value="ENABLED">Enabled</option>
            <option value="DISABLED">Disabled</option>
          </select>
          <button
            type="button"
            onClick={() => setSortAsc((v) => !v)}
            className="inline-flex h-10 items-center gap-2 rounded-lg border border-input px-3 text-sm hover:bg-accent"
          >
            <ArrowDownUp className="size-4" />
            Sort
          </button>
          <button
            type="button"
            onClick={exportCsv}
            className="inline-flex h-10 items-center gap-2 rounded-lg border border-input px-3 text-sm hover:bg-accent"
          >
            <Download className="size-4" />
            Export
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-hidden px-6 pb-6 pt-3">
        {actionError && (
          <div className="mb-3 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {actionError}
          </div>
        )}

        <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-xl border border-border bg-card shadow-sm">
          <div className="grid grid-cols-[minmax(0,1.5fr)_minmax(0,1.3fr)_minmax(0,0.9fr)_minmax(0,0.75fr)_minmax(0,1fr)_100px] items-center gap-3 border-b border-border px-5 py-3 text-xs font-medium tracking-wide text-muted-foreground uppercase">
            <span>Employee Name</span>
            <span>Mail &amp; Phone</span>
            <span>Joining Date</span>
            <span>Status</span>
            <span>System Access</span>
            <span className="text-right">Actions</span>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto">
            {loading && (
              <div className="px-6 py-16 text-center text-sm text-muted-foreground">Loading…</div>
            )}
            {!loading && error && (
              <div className="px-6 py-16 text-center text-sm text-destructive">{error}</div>
            )}
            {!loading && !error && filtered.length === 0 && (
              <div className="px-6 py-16 text-center text-sm text-muted-foreground">
                No employees match your filters.
              </div>
            )}
            {!loading &&
              !error &&
              filtered.map((employe) => (
                <div
                  key={employe.id}
                  className="grid grid-cols-[minmax(0,1.5fr)_minmax(0,1.3fr)_minmax(0,0.9fr)_minmax(0,0.75fr)_minmax(0,1fr)_100px] items-center gap-3 border-b border-border px-5 py-3.5 last:border-b-0 hover:bg-muted/30"
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">
                      {initials(employe.nom)}
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">{employe.nom}</p>
                      <p className="truncate text-xs text-muted-foreground">{employe.poste}</p>
                    </div>
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm">{employe.email}</p>
                    <p className="truncate text-xs text-muted-foreground">
                      {employe.telephone || '—'}
                    </p>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {formatJoiningDate(employe.dateEmbauche)}
                  </p>
                  <span
                    className={`inline-flex w-fit rounded-full px-2.5 py-0.5 text-xs font-medium ${statutBadgeClass(employe.statut)}`}
                  >
                    {statutLabel(employe.statut)}
                  </span>
                  <div className="flex items-center gap-2">
                    <AccessToggle
                      enabled={employe.hasAccess}
                      disabled={togglingId === employe.id}
                      onChange={(next) => void handleToggleAccess(employe, next)}
                    />
                    <span className="text-xs text-muted-foreground">
                      {employe.hasAccess ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  <div className="flex items-center justify-end gap-0.5">
                    <IconBtn label={`View ${employe.nom}`} onClick={() => setView(employe)}>
                      <Eye className="size-4" />
                    </IconBtn>
                    <IconBtn
                      label={`Edit ${employe.nom}`}
                      onClick={() => setForm({ open: true, mode: 'edit', employe })}
                    >
                      <Pencil className="size-4" />
                    </IconBtn>
                    <IconBtn
                      label={`Delete ${employe.nom}`}
                      danger
                      onClick={() => setToDelete(employe)}
                    >
                      <Trash2 className="size-4" />
                    </IconBtn>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </div>

      <EmployeFormDialog
        open={form.open}
        mode={form.open ? form.mode : 'create'}
        initial={form.open && form.mode === 'edit' ? form.employe : null}
        onClose={() => setForm({ open: false })}
        onSubmit={handleSave}
      />

      <AccessEnableDialog
        open={Boolean(accessTarget)}
        employeName={accessTarget?.nom ?? ''}
        onClose={() => setAccessTarget(null)}
        onConfirm={async (userId) => {
          if (!accessTarget) return;
          await setEmployeAccess(accessTarget.id, true, userId);
          await load();
        }}
      />

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Delete employee"
        description={
          toDelete ? `Delete “${toDelete.nom}”? This cannot be undone.` : ''
        }
        confirmLabel="Delete"
        pending={deleting}
        onCancel={() => setToDelete(null)}
        onConfirm={() => void handleDelete()}
      />

      {view && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <button
            type="button"
            className="absolute inset-0 bg-black/40"
            aria-label="Close"
            onClick={() => setView(null)}
          />
          <div className="relative z-10 w-full max-w-md rounded-xl border border-border bg-card p-5 shadow-lg">
            <div className="mb-4 flex items-start gap-3">
              <div className="flex size-12 items-center justify-center rounded-full bg-muted text-sm font-semibold">
                {initials(view.nom)}
              </div>
              <div className="min-w-0 flex-1">
                <h2 className="text-base font-semibold">{view.nom}</h2>
                <p className="text-sm text-muted-foreground">{view.poste}</p>
              </div>
              <button
                type="button"
                className="inline-flex size-8 items-center justify-center rounded-md hover:bg-accent"
                onClick={() => setView(null)}
              >
                <span className="sr-only">Close</span>×
              </button>
            </div>
            <dl className="space-y-2 text-sm">
              <Row label="Email" value={view.email} />
              <Row label="Phone" value={view.telephone || '—'} />
              <Row label="Joined" value={formatJoiningDate(view.dateEmbauche)} />
              <Row label="Status" value={statutLabel(view.statut)} />
              <Row label="System Access" value={view.hasAccess ? 'Enabled' : 'Disabled'} />
            </dl>
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  icon,
  iconWrap,
}: {
  label: string;
  value: number;
  icon: ReactNode;
  iconWrap: string;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="mt-2 text-3xl font-semibold tabular-nums tracking-tight">{value}</p>
        </div>
        <div className={`inline-flex size-9 items-center justify-center rounded-lg ${iconWrap}`}>{icon}</div>
      </div>
    </div>
  );
}

function AccessToggle({
  enabled,
  disabled,
  onChange,
}: {
  enabled: boolean;
  disabled?: boolean;
  onChange: (next: boolean) => void;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={enabled}
      disabled={disabled}
      onClick={() => onChange(!enabled)}
      className={
        enabled
          ? 'relative h-6 w-11 shrink-0 rounded-full bg-sky-500 transition-colors disabled:opacity-50'
          : 'relative h-6 w-11 shrink-0 rounded-full bg-muted transition-colors disabled:opacity-50'
      }
    >
      <span
        className={
          enabled
            ? 'absolute top-0.5 left-0.5 size-5 translate-x-5 rounded-full bg-white shadow transition-transform'
            : 'absolute top-0.5 left-0.5 size-5 translate-x-0 rounded-full bg-white shadow transition-transform'
        }
      />
    </button>
  );
}

function IconBtn({
  label,
  onClick,
  children,
  danger,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
  danger?: boolean;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className={
        danger
          ? 'inline-flex size-8 items-center justify-center rounded-md text-destructive hover:bg-accent'
          : 'inline-flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground'
      }
    >
      {children}
    </button>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 border-b border-border py-2 last:border-0">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
