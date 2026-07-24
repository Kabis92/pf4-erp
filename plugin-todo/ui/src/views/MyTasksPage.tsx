import { useCallback, useEffect, useState } from 'react';
import { Loader2, CheckCircle2, Play, Circle } from 'lucide-react';
import { listMyTasks, statusLabel, updateMyTaskStatus, type Task, type TaskStatus } from '../api/todo';

const NEXT: Record<TaskStatus, TaskStatus | null> = {
  TODO: 'IN_PROGRESS',
  IN_PROGRESS: 'DONE',
  DONE: null,
};

export function MyTasksPage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setTasks(await listMyTasks());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function advance(task: Task) {
    const next = NEXT[task.status];
    if (!next) {
      return;
    }
    setBusyId(task.id);
    setError(null);
    try {
      await updateMyTaskStatus(task.id, next);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Mise à jour impossible');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="flex h-full flex-col overflow-hidden p-6">
      <div className="mb-5">
        <h1 className="text-xl font-semibold">Mes tâches</h1>
        <p className="text-sm text-muted-foreground">
          Tâches qui vous sont assignées — passez-les en cours puis terminées.
        </p>
      </div>

      {error && (
        <div className="mb-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="min-h-0 flex-1 overflow-y-auto rounded-xl border border-border bg-card shadow-sm">
        {loading && (
          <div className="flex items-center justify-center gap-2 px-6 py-16 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" />
            Chargement…
          </div>
        )}
        {!loading && tasks.length === 0 && (
          <div className="px-6 py-16 text-center text-sm text-muted-foreground">
            Aucune tâche assignée pour le moment.
          </div>
        )}
        {!loading &&
          tasks.map((task) => {
            const next = NEXT[task.status];
            return (
              <div
                key={task.id}
                className="flex flex-col gap-3 border-b border-border px-5 py-4 last:border-b-0 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusIcon status={task.status} />
                    <p className="truncate text-sm font-semibold">{task.title}</p>
                    <span className="rounded-md border border-border px-2 py-0.5 text-xs">
                      {statusLabel(task.status)}
                    </span>
                  </div>
                  {task.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{task.description}</p>
                  )}
                </div>
                {next && (
                  <button
                    type="button"
                    disabled={busyId === task.id}
                    onClick={() => void advance(task)}
                    className="inline-flex h-9 shrink-0 items-center gap-2 rounded-md bg-primary px-3 text-sm text-primary-foreground hover:opacity-90 disabled:opacity-50"
                  >
                    {busyId === task.id ? <Loader2 className="size-4 animate-spin" /> : null}
                    {next === 'IN_PROGRESS' ? 'Démarrer' : 'Terminer'}
                  </button>
                )}
              </div>
            );
          })}
      </div>
    </div>
  );
}

function StatusIcon({ status }: { status: TaskStatus }) {
  if (status === 'DONE') {
    return <CheckCircle2 className="size-4 shrink-0" />;
  }
  if (status === 'IN_PROGRESS') {
    return <Play className="size-4 shrink-0" />;
  }
  return <Circle className="size-4 shrink-0" />;
}
