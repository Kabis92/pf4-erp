import { FormEvent, useEffect, useId, useState } from 'react';
import { X } from 'lucide-react';
import { listDirectoryUsers, type UserOption } from '../api/employes';

type Props = {
  open: boolean;
  employeName: string;
  onClose: () => void;
  onConfirm: (userId: string) => Promise<void>;
};

export function AccessEnableDialog({ open, employeName, onClose, onConfirm }: Props) {
  const titleId = useId();
  const [users, setUsers] = useState<UserOption[]>([]);
  const [userId, setUserId] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setUserId('');
    setError(null);
    setSaving(false);
    void listDirectoryUsers()
      .then(setUsers)
      .catch(() => setUsers([]));
  }, [open]);

  if (!open) return null;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!userId) {
      setError('Select an account');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await onConfirm(userId);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to enable access');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button type="button" className="absolute inset-0 bg-black/40" aria-label="Close" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative z-10 w-full max-w-md rounded-xl border border-border bg-card p-5 shadow-lg"
      >
        <div className="mb-4 flex items-start justify-between gap-3">
          <div>
            <h2 id={titleId} className="text-base font-semibold">
              Enable system access
            </h2>
            <p className="text-sm text-muted-foreground">
              Link a login account for <span className="font-medium text-foreground">{employeName}</span>.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="inline-flex size-8 items-center justify-center rounded-md hover:bg-accent"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>
        <form className="space-y-3" onSubmit={(e) => void handleSubmit(e)}>
          <label className="block space-y-1.5">
            <span className="text-sm font-medium">Account</span>
            <select
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
              required
            >
              <option value="">Select user…</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.displayName} (@{u.username})
                </option>
              ))}
            </select>
          </label>
          {error && <p className="text-sm text-destructive">{error}</p>}
          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="inline-flex h-9 items-center rounded-md border border-input px-4 text-sm hover:bg-accent"
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm text-primary-foreground hover:opacity-90 disabled:opacity-50"
            >
              {saving ? 'Enabling…' : 'Enable'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
