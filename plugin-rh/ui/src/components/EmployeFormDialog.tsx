import { FormEvent, useEffect, useId, useState, type ReactNode } from 'react';
import { X } from 'lucide-react';
import type { Employe, EmployeInput } from '../api/employes';

type Mode = 'create' | 'edit';

type Props = {
  open: boolean;
  mode: Mode;
  initial?: Employe | null;
  onClose: () => void;
  onSubmit: (input: EmployeInput) => Promise<void>;
};

const empty: EmployeInput = {
  nom: '',
  email: '',
  poste: '',
  dateEmbauche: new Date().toISOString().slice(0, 10),
};

export function EmployeFormDialog({ open, mode, initial, onClose, onSubmit }: Props) {
  const titleId = useId();
  const [form, setForm] = useState<EmployeInput>(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    setSaving(false);
    if (mode === 'edit' && initial) {
      setForm({
        nom: initial.nom,
        email: initial.email,
        poste: initial.poste,
        dateEmbauche: initial.dateEmbauche,
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
        poste: form.poste.trim(),
        dateEmbauche: form.dateEmbauche,
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
      <button
        type="button"
        className="absolute inset-0 bg-black/40"
        aria-label="Fermer"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative z-10 w-full max-w-md rounded-xl border border-border bg-card p-5 shadow-lg"
      >
        <div className="mb-4 flex items-start justify-between gap-3">
          <div>
            <h2 id={titleId} className="text-base font-semibold">
              {mode === 'create' ? 'Nouvel employé' : 'Modifier l’employé'}
            </h2>
            <p className="text-sm text-muted-foreground">
              {mode === 'create' ? 'Renseignez les informations du collaborateur.' : 'Mettez à jour la fiche.'}
            </p>
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

        <form className="space-y-3" onSubmit={(e) => void handleSubmit(e)}>
          <Field label="Nom">
            <input
              required
              value={form.nom}
              onChange={(e) => setForm((f) => ({ ...f, nom: e.target.value }))}
              className={inputClass}
              placeholder="Ada Lovelace"
              autoFocus
            />
          </Field>
          <Field label="Email">
            <input
              required
              type="email"
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              className={inputClass}
              placeholder="ada@example.com"
            />
          </Field>
          <Field label="Poste">
            <input
              required
              value={form.poste}
              onChange={(e) => setForm((f) => ({ ...f, poste: e.target.value }))}
              className={inputClass}
              placeholder="Ingénieure"
            />
          </Field>
          <Field label="Date d’embauche">
            <input
              required
              type="date"
              value={form.dateEmbauche}
              onChange={(e) => setForm((f) => ({ ...f, dateEmbauche: e.target.value }))}
              className={inputClass}
            />
          </Field>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="inline-flex h-9 items-center rounded-md border border-input bg-background px-4 text-sm hover:bg-accent"
              disabled={saving}
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              {saving ? 'Enregistrement…' : mode === 'create' ? 'Créer' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
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
