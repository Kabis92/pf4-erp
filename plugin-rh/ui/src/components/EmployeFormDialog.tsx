import { FormEvent, useEffect, useId, useState, type ReactNode } from 'react';
import { X } from 'lucide-react';
import {
  listDirectoryUsers,
  type Employe,
  type EmployeInput,
  type EmployeStatut,
  type UserOption,
} from '../api/employes';

type Mode = 'create' | 'edit';

type Props = {
  open: boolean;
  mode: Mode;
  initial?: Employe | null;
  onClose: () => void;
  onSubmit: (input: EmployeInput) => Promise<void>;
};

const STATUTS: EmployeStatut[] = ['ACTIF', 'CONGE', 'INACTIF'];

const empty: EmployeInput = {
  nom: '',
  email: '',
  telephone: null,
  poste: '',
  dateEmbauche: new Date().toISOString().slice(0, 10),
  userId: null,
  statut: 'ACTIF',
};

export function EmployeFormDialog({ open, mode, initial, onClose, onSubmit }: Props) {
  const titleId = useId();
  const [form, setForm] = useState<EmployeInput>(empty);
  const [users, setUsers] = useState<UserOption[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    setSaving(false);
    void listDirectoryUsers()
      .then(setUsers)
      .catch(() => setUsers([]));
    if (mode === 'edit' && initial) {
      setForm({
        nom: initial.nom,
        email: initial.email,
        telephone: initial.telephone,
        poste: initial.poste,
        dateEmbauche: initial.dateEmbauche,
        userId: initial.userId,
        statut: normalizeStatut(initial.statut),
      });
    } else {
      setForm(empty);
    }
  }, [open, mode, initial]);

  if (!open) {
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({
        nom: form.nom.trim(),
        email: form.email.trim(),
        telephone: form.telephone?.trim() || null,
        poste: form.poste.trim(),
        dateEmbauche: form.dateEmbauche,
        userId: form.userId || null,
        statut: form.statut,
      });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enregistrement impossible');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button type="button" className="absolute inset-0 bg-black/40" aria-label="Fermer" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative z-10 flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-xl border border-border bg-card shadow-lg"
      >
        <div className="shrink-0 border-b border-border p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 id={titleId} className="text-base font-semibold">
                {mode === 'create' ? 'Add Employee' : 'Edit Employee'}
              </h2>
              <p className="text-sm text-muted-foreground">Profile, status and system access.</p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="inline-flex size-8 items-center justify-center rounded-md hover:bg-accent"
              aria-label="Fermer"
            >
              <X className="size-4" />
            </button>
          </div>
        </div>

        <form className="space-y-3 overflow-y-auto p-5" onSubmit={(e) => void handleSubmit(e)}>
          <Field label="Full name">
            <input
              required
              value={form.nom}
              onChange={(e) => setForm((f) => ({ ...f, nom: e.target.value }))}
              className={inputClass}
              placeholder="Khalid Ali"
              autoFocus
            />
          </Field>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Email">
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                className={inputClass}
                placeholder="khalid@company.com"
              />
            </Field>
            <Field label="Phone">
              <input
                value={form.telephone ?? ''}
                onChange={(e) => setForm((f) => ({ ...f, telephone: e.target.value }))}
                className={inputClass}
                placeholder="+966 561234567"
              />
            </Field>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Position">
              <input
                required
                value={form.poste}
                onChange={(e) => setForm((f) => ({ ...f, poste: e.target.value }))}
                className={inputClass}
                placeholder="Product Manager"
              />
            </Field>
            <Field label="Joining date">
              <input
                required
                type="date"
                value={form.dateEmbauche}
                onChange={(e) => setForm((f) => ({ ...f, dateEmbauche: e.target.value }))}
                className={inputClass}
              />
            </Field>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Status">
              <select
                value={form.statut}
                onChange={(e) => setForm((f) => ({ ...f, statut: e.target.value as EmployeStatut }))}
                className={inputClass}
              >
                {STATUTS.map((s) => (
                  <option key={s} value={s}>
                    {s === 'ACTIF' ? 'Active' : s === 'CONGE' ? 'On Leave' : 'Inactive'}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="System access (account)">
              <select
                value={form.userId ?? ''}
                onChange={(e) =>
                  setForm((f) => ({ ...f, userId: e.target.value ? e.target.value : null }))
                }
                className={inputClass}
              >
                <option value="">Disabled — no account</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName} (@{u.username})
                  </option>
                ))}
              </select>
            </Field>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm hover:bg-accent"
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              {saving ? 'Saving…' : mode === 'create' ? 'Add Employee' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function normalizeStatut(statut: string): EmployeStatut {
  if (statut === 'CONGE') return 'CONGE';
  if (statut === 'INACTIF' || statut === 'ONBOARDING' || statut === 'SORTIE') return 'INACTIF';
  return 'ACTIF';
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium">{label}</span>
      {children}
    </label>
  );
}

const inputClass =
  'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-1 focus-visible:ring-ring';
