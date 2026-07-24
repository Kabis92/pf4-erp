import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Pencil,
  Plus,
  Search,
  Trash2,
} from 'lucide-react';
import {
  createEmploye,
  deleteEmploye,
  listEmployes,
  updateEmploye,
  type Employe,
  type EmployeInput,
} from '../api/employes';
import { EmployeFormDialog } from '../components/EmployeFormDialog';
import { ConfirmDialog } from '../components/ConfirmDialog';

function initials(name: string) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

const ROW_COLS =
  'grid grid-cols-[minmax(0,2fr)_minmax(0,2fr)_minmax(0,1.2fr)_minmax(0,1fr)_88px] items-center gap-4 px-5';

type FormState =
  | { open: false }
  | { open: true; mode: 'create' }
  | { open: true; mode: 'edit'; employe: Employe };

export function EmployeListPage() {
  const [employes, setEmployes] = useState<Employe[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>({ open: false });
  const [toDelete, setToDelete] = useState<Employe | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [sortAsc, setSortAsc] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setEmployes(await listEmployes());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur de chargement');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    let rows = employes;
    if (q) {
      rows = rows.filter(
        (e) =>
          e.nom.toLowerCase().includes(q) ||
          e.email.toLowerCase().includes(q) ||
          e.poste.toLowerCase().includes(q),
      );
    }
    return [...rows].sort((a, b) => {
      const cmp = a.nom.localeCompare(b.nom, 'fr', { sensitivity: 'base' });
      return sortAsc ? cmp : -cmp;
    });
  }, [employes, search, sortAsc]);

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
    if (!toDelete) {
      return;
    }
    setDeleting(true);
    setActionError(null);
    try {
      await deleteEmploye(toDelete.id);
      setToDelete(null);
      await load();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="flex h-full flex-col overflow-hidden p-6">
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative w-full max-w-md">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="search"
            placeholder="Rechercher des employés..."
            className="flex h-9 w-full rounded-md border border-input bg-transparent py-1 pr-3 pl-9 text-sm shadow-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="flex shrink-0 flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setForm({ open: true, mode: 'create' })}
            className="inline-flex h-8 items-center gap-2 rounded-md bg-primary px-3 text-sm text-primary-foreground hover:opacity-90"
          >
            <Plus className="size-4" />
            Ajouter
          </button>
        </div>
      </div>

      {actionError && (
        <div className="mb-3 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {actionError}
        </div>
      )}

      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border bg-card shadow-sm">
        <div
          className={`${ROW_COLS} border-b border-border py-3 text-xs font-medium tracking-wider text-muted-foreground uppercase`}
        >
          <button
            type="button"
            className="flex items-center gap-1 text-left hover:text-foreground"
            onClick={() => setSortAsc((v) => !v)}
          >
            Nom
            <ArrowUpDown className="size-3.5" />
          </button>
          <span>Email</span>
          <span>Poste</span>
          <span>Embauche</span>
          <span className="text-right">Actions</span>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto">
          {loading && (
            <div className="px-6 py-16 text-center text-sm text-muted-foreground">Chargement…</div>
          )}
          {!loading && error && (
            <div className="px-6 py-16 text-center text-sm text-destructive">{error}</div>
          )}
          {!loading && !error && filtered.length === 0 && (
            <div className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center">
              <div>
                <p className="text-sm font-medium">Aucun employé trouvé</p>
                <p className="text-sm text-muted-foreground">
                  {search ? 'Ajustez la recherche ou créez un employé.' : 'Commencez par ajouter un collaborateur.'}
                </p>
              </div>
              {!search && (
                <button
                  type="button"
                  onClick={() => setForm({ open: true, mode: 'create' })}
                  className="inline-flex h-8 items-center gap-2 rounded-md bg-primary px-3 text-sm text-primary-foreground hover:opacity-90"
                >
                  <Plus className="size-4" />
                  Ajouter un employé
                </button>
              )}
            </div>
          )}
          {!loading &&
            !error &&
            filtered.map((employe) => (
              <div
                key={employe.id}
                className={`${ROW_COLS} border-b border-border py-4 transition-colors last:border-b-0 hover:bg-muted/40`}
              >
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-medium">
                    {initials(employe.nom)}
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{employe.nom}</p>
                    <p className="truncate text-xs text-muted-foreground">ID #{employe.id.slice(0, 8)}</p>
                  </div>
                </div>
                <p className="truncate text-sm text-muted-foreground">{employe.email}</p>
                <span className="inline-flex w-fit rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                  {employe.poste}
                </span>
                <p className="text-sm text-muted-foreground">{employe.dateEmbauche}</p>
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    className="inline-flex size-8 items-center justify-center rounded-md hover:bg-accent"
                    aria-label={`Modifier ${employe.nom}`}
                    onClick={() => setForm({ open: true, mode: 'edit', employe })}
                  >
                    <Pencil className="size-4" />
                  </button>
                  <button
                    type="button"
                    className="inline-flex size-8 items-center justify-center rounded-md text-destructive hover:bg-accent"
                    aria-label={`Supprimer ${employe.nom}`}
                    onClick={() => setToDelete(employe)}
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              </div>
            ))}
        </div>

        <div className="flex shrink-0 items-center justify-between border-t border-border px-5 py-3">
          <p className="text-sm text-muted-foreground">
            Affichage de {filtered.length} sur {employes.length} employés
          </p>
          <div className="flex items-center gap-1">
            <button
              type="button"
              disabled
              className="inline-flex size-8 items-center justify-center rounded-md border border-input opacity-50"
            >
              <ChevronLeft className="size-4" />
            </button>
            <button
              type="button"
              className="inline-flex size-8 items-center justify-center rounded-md bg-primary text-sm text-primary-foreground"
            >
              1
            </button>
            <button
              type="button"
              disabled
              className="inline-flex size-8 items-center justify-center rounded-md border border-input opacity-50"
            >
              <ChevronRight className="size-4" />
            </button>
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

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Supprimer l’employé"
        description={
          toDelete
            ? `Confirmer la suppression de « ${toDelete.nom} » ? Cette action est définitive.`
            : ''
        }
        confirmLabel="Supprimer"
        pending={deleting}
        onCancel={() => setToDelete(null)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
