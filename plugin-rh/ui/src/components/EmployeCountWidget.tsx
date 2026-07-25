import { useEffect, useState } from 'react';
import { countEmployes } from '../api/employes';

export function EmployeCountWidget() {
  const [count, setCount] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    countEmployes()
      .then((data) => {
        if (!cancelled) {
          setCount(data.count);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="rounded-xl border border-border bg-card p-5 shadow-sm">
      <p className="text-xs font-medium tracking-wider text-muted-foreground uppercase">RH</p>
      <p className="mt-2 text-3xl font-semibold tabular-nums">
        {error ? '—' : count === null ? '…' : count}
      </p>
      <p className="mt-1 text-sm text-muted-foreground">
        {error ? `Erreur: ${error}` : 'Effectif · cycle de vie'}
      </p>
    </div>
  );
}
