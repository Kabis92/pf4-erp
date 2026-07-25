import { FormEvent, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  Check,
  CheckCircle2,
  GripVertical,
  Heart,
  ListTodo,
  Loader2,
  MoreHorizontal,
  Plus,
  Search,
  Star,
  Trash2,
  User,
} from 'lucide-react';
import {
  canManageTodo,
  createTask,
  listEmployesForAssign,
  listMyTasks,
  listProjects,
  listTasks,
  projectBorderClass,
  projectTagClass,
  restoreTask,
  setMyFavorite,
  setTaskFavorite,
  trashTask,
  updateMyTaskStatus,
  updateTaskStatus,
  type EmployeOption,
  type Project,
  type Task,
  type TaskStatus,
} from '../api/todo';

type FilterId = 'ALL' | 'MINE' | 'FAVORITES' | 'DONE' | 'DELETED';
type OrderBy = 'title' | 'updated' | 'status';

function initials(name: string) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}

function normalize(t: Task): Task {
  return {
    ...t,
    favorite: Boolean(t.favorite),
    deleted: Boolean(t.deleted),
    projectName: t.projectName ?? 'Project',
  };
}

export function MyTasksPage() {
  const manage = canManageTodo();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [employes, setEmployes] = useState<EmployeOption[]>([]);
  const [myEmployeId, setMyEmployeId] = useState<string | null>(null);
  const [filter, setFilter] = useState<FilterId>('ALL');
  const [orderBy, setOrderBy] = useState<OrderBy>('updated');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [menuId, setMenuId] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (manage) {
        const [all, mine, projs, emps] = await Promise.all([
          listTasks(),
          listMyTasks().catch(() => [] as Task[]),
          listProjects(),
          listEmployesForAssign().catch(() => [] as EmployeOption[]),
        ]);
        setTasks(all.map(normalize));
        setProjects(projs);
        setEmployes(emps);
        setMyEmployeId(mine[0]?.assigneeEmployeId ?? null);
      } else {
        const mine = await listMyTasks();
        setTasks(mine.map(normalize));
        setMyEmployeId(mine[0]?.assigneeEmployeId ?? null);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, [manage]);

  useEffect(() => {
    void load();
  }, [load]);

  const counts = useMemo(() => {
    const active = tasks.filter((t) => !t.deleted);
    return {
      all: active.length,
      mine: active.filter((t) => myEmployeId && t.assigneeEmployeId === myEmployeId).length,
      favorites: active.filter((t) => t.favorite).length,
      done: active.filter((t) => t.status === 'DONE').length,
      deleted: tasks.filter((t) => t.deleted).length,
    };
  }, [tasks, myEmployeId]);

  const filtered = useMemo(() => {
    let rows = [...tasks];
    switch (filter) {
      case 'ALL':
        rows = rows.filter((t) => !t.deleted);
        break;
      case 'MINE':
        rows = rows.filter((t) => !t.deleted && myEmployeId && t.assigneeEmployeId === myEmployeId);
        break;
      case 'FAVORITES':
        rows = rows.filter((t) => !t.deleted && t.favorite);
        break;
      case 'DONE':
        rows = rows.filter((t) => !t.deleted && t.status === 'DONE');
        break;
      case 'DELETED':
        rows = rows.filter((t) => t.deleted);
        break;
    }
    const q = search.trim().toLowerCase();
    if (q) {
      rows = rows.filter(
        (t) =>
          t.title.toLowerCase().includes(q) ||
          t.description.toLowerCase().includes(q) ||
          (t.projectName ?? '').toLowerCase().includes(q),
      );
    }
    rows.sort((a, b) => {
      if (orderBy === 'title') return a.title.localeCompare(b.title);
      if (orderBy === 'status') return a.status.localeCompare(b.status);
      return b.updatedAt.localeCompare(a.updatedAt);
    });
    return rows;
  }, [tasks, filter, search, orderBy, myEmployeId]);

  async function toggleDone(task: Task) {
    setBusyId(task.id);
    setError(null);
    try {
      const next: TaskStatus = task.status === 'DONE' ? 'TODO' : 'DONE';
      if (manage) {
        await updateTaskStatus(task.id, next);
      } else {
        await updateMyTaskStatus(task.id, next);
      }
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Mise à jour impossible');
    } finally {
      setBusyId(null);
    }
  }

  async function toggleFavorite(task: Task) {
    setBusyId(task.id);
    setError(null);
    try {
      if (manage) {
        await setTaskFavorite(task.id, !task.favorite);
      } else {
        await setMyFavorite(task.id, !task.favorite);
      }
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Favori impossible');
    } finally {
      setBusyId(null);
    }
  }

  async function moveToTrash(task: Task) {
    if (!manage) return;
    setBusyId(task.id);
    setMenuId(null);
    try {
      await trashTask(task.id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setBusyId(null);
    }
  }

  async function restore(task: Task) {
    if (!manage) return;
    setBusyId(task.id);
    setMenuId(null);
    try {
      await restoreTask(task.id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Restauration impossible');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="flex h-full overflow-hidden bg-background">
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <div className="flex shrink-0 items-center justify-between gap-3 border-b border-border px-6 py-4">
          <h1 className="text-xl font-semibold tracking-tight">Todo List</h1>
          {manage && (
            <button
              type="button"
              onClick={() => setAddOpen(true)}
              className="inline-flex h-9 items-center gap-2 rounded-lg bg-primary px-3 text-sm font-medium text-primary-foreground hover:opacity-90"
            >
              <Plus className="size-4" />
              Add Task
            </button>
          )}
        </div>

        <div className="flex shrink-0 flex-col gap-3 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap items-center gap-2">
            <select
              value={orderBy}
              onChange={(e) => setOrderBy(e.target.value as OrderBy)}
              className="h-9 rounded-lg border border-input bg-transparent px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <option value="updated">Order by · Recent</option>
              <option value="title">Order by · Title</option>
              <option value="status">Order by · Status</option>
            </select>
          </div>
          <div className="relative w-full sm:max-w-xs">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="search"
              placeholder="Search todo list"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex h-9 w-full rounded-lg border border-input bg-transparent py-1 pr-3 pl-9 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>
        </div>

        {error && (
          <div className="mx-6 mb-3 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="min-h-0 flex-1 overflow-y-auto px-6 pb-6">
          <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
            {loading && (
              <div className="flex items-center justify-center gap-2 px-6 py-16 text-sm text-muted-foreground">
                <Loader2 className="size-4 animate-spin" />
                Loading…
              </div>
            )}
            {!loading && filtered.length === 0 && (
              <div className="px-6 py-16 text-center text-sm text-muted-foreground">
                No tasks in this view.
              </div>
            )}
            {!loading &&
              filtered.map((task) => {
                const tag = task.projectName ?? 'Project';
                const done = task.status === 'DONE';
                return (
                  <div
                    key={task.id}
                    className={`group relative flex items-center gap-3 border-b border-border border-l-4 px-4 py-3.5 last:border-b-0 hover:bg-muted/30 ${projectBorderClass(tag)}`}
                  >
                    <GripVertical className="size-4 shrink-0 text-muted-foreground/50" />
                    <button
                      type="button"
                      disabled={busyId === task.id || task.deleted}
                      onClick={() => void toggleDone(task)}
                      className={
                        done
                          ? 'flex size-5 shrink-0 items-center justify-center rounded-md bg-sky-500 text-white'
                          : 'flex size-5 shrink-0 items-center justify-center rounded-md border border-input bg-background'
                      }
                      aria-label={done ? 'Mark incomplete' : 'Mark done'}
                    >
                      {done ? <Check className="size-3.5" /> : null}
                    </button>
                    <button
                      type="button"
                      disabled={busyId === task.id || task.deleted}
                      onClick={() => void toggleFavorite(task)}
                      className="shrink-0 text-muted-foreground hover:text-amber-500"
                      aria-label="Favorite"
                    >
                      <Star
                        className={
                          task.favorite
                            ? 'size-4 fill-amber-400 text-amber-400'
                            : 'size-4'
                        }
                      />
                    </button>
                    <div className="min-w-0 flex-1">
                      <p
                        className={
                          done
                            ? 'truncate text-sm text-muted-foreground line-through'
                            : 'truncate text-sm font-medium'
                        }
                      >
                        {task.title}
                      </p>
                    </div>
                    <span
                      className={`hidden shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium sm:inline-flex ${projectTagClass(tag)}`}
                    >
                      {tag}
                    </span>
                    {task.assigneeName ? (
                      <div
                        className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-[10px] font-semibold"
                        title={task.assigneeName}
                      >
                        {initials(task.assigneeName)}
                      </div>
                    ) : (
                      <div className="size-8 shrink-0" />
                    )}
                    <div className="relative shrink-0">
                      <button
                        type="button"
                        className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent"
                        onClick={() => setMenuId((id) => (id === task.id ? null : task.id))}
                        aria-label="More"
                      >
                        <MoreHorizontal className="size-4" />
                      </button>
                      {menuId === task.id && (
                        <div className="absolute top-9 right-0 z-20 w-40 rounded-lg border border-border bg-card py-1 shadow-lg">
                          {task.deleted && manage ? (
                            <button
                              type="button"
                              className="flex w-full px-3 py-2 text-left text-sm hover:bg-accent"
                              onClick={() => void restore(task)}
                            >
                              Restore
                            </button>
                          ) : manage ? (
                            <button
                              type="button"
                              className="flex w-full px-3 py-2 text-left text-sm text-destructive hover:bg-accent"
                              onClick={() => void moveToTrash(task)}
                            >
                              Move to trash
                            </button>
                          ) : null}
                          {!manage && (
                            <p className="px-3 py-2 text-xs text-muted-foreground">No actions</p>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      </div>

      <aside className="hidden w-56 shrink-0 border-l border-border bg-card/50 p-4 lg:block">
        <p className="mb-3 text-xs font-medium tracking-wide text-muted-foreground uppercase">
          Filters
        </p>
        <nav className="space-y-1">
          <FilterItem
            active={filter === 'ALL'}
            icon={<ListTodo className="size-4" />}
            label="All"
            count={counts.all}
            onClick={() => setFilter('ALL')}
          />
          <FilterItem
            active={filter === 'MINE'}
            icon={<User className="size-4" />}
            label="My Task"
            count={counts.mine}
            onClick={() => setFilter('MINE')}
          />
          <FilterItem
            active={filter === 'FAVORITES'}
            icon={<Heart className="size-4" />}
            label="Favorites"
            count={counts.favorites}
            onClick={() => setFilter('FAVORITES')}
          />
          <FilterItem
            active={filter === 'DONE'}
            icon={<CheckCircle2 className="size-4" />}
            label="Done"
            count={counts.done}
            onClick={() => setFilter('DONE')}
          />
          {manage && (
            <FilterItem
              active={filter === 'DELETED'}
              icon={<Trash2 className="size-4" />}
              label="Deleted"
              count={counts.deleted}
              onClick={() => setFilter('DELETED')}
            />
          )}
        </nav>
      </aside>

      {addOpen && manage && (
        <AddTaskDialog
          projects={projects}
          employes={employes}
          onClose={() => setAddOpen(false)}
          onCreated={async () => {
            setAddOpen(false);
            await load();
          }}
        />
      )}
    </div>
  );
}

function FilterItem({
  active,
  icon,
  label,
  count,
  onClick,
}: {
  active: boolean;
  icon: ReactNode;
  label: string;
  count: number;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        active
          ? 'flex w-full items-center gap-2 rounded-lg bg-primary/10 px-3 py-2 text-sm font-medium text-foreground'
          : 'flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-muted-foreground hover:bg-muted/60 hover:text-foreground'
      }
    >
      {icon}
      <span className="flex-1 text-left">{label}</span>
      <span className="rounded-full bg-muted px-1.5 text-xs tabular-nums">{count}</span>
    </button>
  );
}

function AddTaskDialog({
  projects,
  employes,
  onClose,
  onCreated,
}: {
  projects: Project[];
  employes: EmployeOption[];
  onClose: () => void;
  onCreated: () => Promise<void>;
}) {
  const [title, setTitle] = useState('');
  const [projectId, setProjectId] = useState(projects[0]?.id ?? '');
  const [assignee, setAssignee] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!projectId) {
      setError('Create a project in Gestion first');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await createTask({
        projectId,
        title: title.trim(),
        assigneeEmployeId: assignee || null,
      });
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button type="button" className="absolute inset-0 bg-black/40" aria-label="Close" onClick={onClose} />
      <form
        onSubmit={(e) => void submit(e)}
        className="relative z-10 w-full max-w-md space-y-3 rounded-xl border border-border bg-card p-5 shadow-lg"
      >
        <h2 className="text-base font-semibold">Add Task</h2>
        <input
          required
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Task title"
          className="flex h-9 w-full rounded-md border border-input px-3 text-sm outline-none focus-visible:ring-1 focus-visible:ring-ring"
          autoFocus
        />
        <select
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input px-3 text-sm"
          required
        >
          {projects.length === 0 && <option value="">No project</option>}
          {projects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <select
          value={assignee}
          onChange={(e) => setAssignee(e.target.value)}
          className="flex h-9 w-full rounded-md border border-input px-3 text-sm"
        >
          <option value="">Unassigned</option>
          {employes.map((e) => (
            <option key={e.id} value={e.id}>
              {e.nom}
            </option>
          ))}
        </select>
        {error && <p className="text-sm text-destructive">{error}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="h-9 rounded-md border border-input px-3 text-sm">
            Cancel
          </button>
          <button
            type="submit"
            disabled={saving}
            className="h-9 rounded-md bg-primary px-3 text-sm text-primary-foreground disabled:opacity-50"
          >
            {saving ? 'Saving…' : 'Create'}
          </button>
        </div>
      </form>
    </div>
  );
}
