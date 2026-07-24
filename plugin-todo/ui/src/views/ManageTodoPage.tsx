import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Loader2, Pencil, Plus, Trash2, X } from 'lucide-react';
import {
  createProject,
  createTask,
  deleteProject,
  deleteTask,
  listEmployesForAssign,
  listProjects,
  listTasks,
  statusLabel,
  updateTask,
  updateTaskStatus,
  type EmployeOption,
  type Project,
  type Task,
  type TaskStatus,
} from '../api/todo';

const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export function ManageTodoPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [employes, setEmployes] = useState<EmployeOption[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [projectName, setProjectName] = useState('');
  const [taskTitle, setTaskTitle] = useState('');
  const [taskAssignee, setTaskAssignee] = useState('');
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState<Task | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editAssignee, setEditAssignee] = useState('');
  const [editStatus, setEditStatus] = useState<TaskStatus>('TODO');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [p, e] = await Promise.all([listProjects(), listEmployesForAssign()]);
      setProjects(p);
      setEmployes(e);
      const projectId =
        selectedProjectId && p.some((x) => x.id === selectedProjectId)
          ? selectedProjectId
          : (p[0]?.id ?? null);
      setSelectedProjectId(projectId);
      setTasks(projectId ? await listTasks(projectId) : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, [selectedProjectId]);

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refreshTasks(projectId: string) {
    setTasks(await listTasks(projectId));
  }

  async function selectProject(id: string) {
    setSelectedProjectId(id);
    setError(null);
    setEditing(null);
    try {
      await refreshTasks(id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement tâches impossible');
    }
  }

  async function onCreateProject(e: FormEvent) {
    e.preventDefault();
    if (!projectName.trim()) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const created = await createProject({ name: projectName.trim() });
      setProjectName('');
      await load();
      setSelectedProjectId(created.id);
      await refreshTasks(created.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Création projet impossible');
    } finally {
      setBusy(false);
    }
  }

  async function onCreateTask(e: FormEvent) {
    e.preventDefault();
    if (!selectedProjectId || !taskTitle.trim()) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await createTask({
        projectId: selectedProjectId,
        title: taskTitle.trim(),
        assigneeEmployeId: taskAssignee || null,
      });
      setTaskTitle('');
      setTaskAssignee('');
      await refreshTasks(selectedProjectId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Création tâche impossible');
    } finally {
      setBusy(false);
    }
  }

  function openEdit(task: Task) {
    setEditing(task);
    setEditTitle(task.title);
    setEditAssignee(task.assigneeEmployeId ?? '');
    setEditStatus(task.status);
  }

  async function onSaveEdit(e: FormEvent) {
    e.preventDefault();
    if (!editing || !selectedProjectId || !editTitle.trim()) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await updateTask(editing.id, {
        title: editTitle.trim(),
        description: editing.description,
        assigneeEmployeId: editAssignee || null,
      });
      if (editStatus !== editing.status) {
        await updateTaskStatus(editing.id, editStatus);
      }
      setEditing(null);
      await refreshTasks(selectedProjectId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Enregistrement impossible');
    } finally {
      setBusy(false);
    }
  }

  async function onDeleteProject(id: string) {
    if (!window.confirm('Supprimer ce projet (sans tâches) ?')) {
      return;
    }
    setBusy(true);
    try {
      await deleteProject(id);
      setSelectedProjectId(null);
      setEditing(null);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setBusy(false);
    }
  }

  async function onDeleteTask(id: string) {
    if (!selectedProjectId) {
      return;
    }
    setBusy(true);
    try {
      await deleteTask(id);
      if (editing?.id === id) {
        setEditing(null);
      }
      await refreshTasks(selectedProjectId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Suppression impossible');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex h-full flex-col overflow-hidden p-6">
      <div className="mb-5">
        <h1 className="text-xl font-semibold">Gestion Todo</h1>
        <p className="text-sm text-muted-foreground">
          Projets, tâches, statut et assignation aux employés liés à un compte.
        </p>
      </div>

      {error && (
        <div className="mb-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="grid min-h-0 flex-1 gap-4 lg:grid-cols-[280px_1fr]">
        <div className="flex min-h-0 flex-col overflow-hidden rounded-xl border border-border bg-card">
          <form onSubmit={onCreateProject} className="border-b border-border p-3">
            <input
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              placeholder="Nouveau projet"
              className="mb-2 flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm"
            />
            <button
              type="submit"
              disabled={busy}
              className="inline-flex h-8 w-full items-center justify-center gap-1 rounded-md bg-primary text-xs text-primary-foreground"
            >
              <Plus className="size-3.5" />
              Créer le projet
            </button>
          </form>
          <div className="min-h-0 flex-1 overflow-y-auto">
            {loading && (
              <div className="flex justify-center py-8">
                <Loader2 className="size-4 animate-spin text-muted-foreground" />
              </div>
            )}
            {!loading &&
              projects.map((project) => (
                <div
                  key={project.id}
                  className={`flex items-center gap-1 border-b border-border last:border-b-0 ${
                    selectedProjectId === project.id ? 'bg-muted/50' : ''
                  }`}
                >
                  <button
                    type="button"
                    className="min-w-0 flex-1 px-3 py-3 text-left text-sm hover:bg-muted/40"
                    onClick={() => void selectProject(project.id)}
                  >
                    <p className="truncate font-medium">{project.name}</p>
                  </button>
                  <button
                    type="button"
                    className="mr-2 inline-flex size-8 items-center justify-center rounded-md text-destructive hover:bg-accent"
                    onClick={() => void onDeleteProject(project.id)}
                    aria-label="Supprimer"
                  >
                    <Trash2 className="size-3.5" />
                  </button>
                </div>
              ))}
          </div>
        </div>

        <div className="flex min-h-0 flex-col overflow-hidden rounded-xl border border-border bg-card">
          {!selectedProjectId && (
            <div className="px-5 py-16 text-center text-sm text-muted-foreground">
              Sélectionnez ou créez un projet.
            </div>
          )}
          {selectedProjectId && (
            <>
              <form
                onSubmit={onCreateTask}
                className="grid gap-2 border-b border-border p-4 sm:grid-cols-[1fr_180px_auto]"
              >
                <input
                  value={taskTitle}
                  onChange={(e) => setTaskTitle(e.target.value)}
                  placeholder="Titre de la tâche"
                  className="flex h-9 rounded-md border border-input bg-transparent px-3 text-sm"
                  required
                />
                <select
                  value={taskAssignee}
                  onChange={(e) => setTaskAssignee(e.target.value)}
                  className="flex h-9 rounded-md border border-input bg-transparent px-3 text-sm"
                >
                  <option value="">Non assignée</option>
                  {employes.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.nom}
                    </option>
                  ))}
                </select>
                <button
                  type="submit"
                  disabled={busy}
                  className="inline-flex h-9 items-center justify-center gap-1 rounded-md bg-primary px-3 text-sm text-primary-foreground"
                >
                  <Plus className="size-4" />
                  Ajouter
                </button>
              </form>

              {employes.length === 0 && (
                <p className="border-b border-border px-4 py-2 text-xs text-muted-foreground">
                  Aucun employé lié à un compte — liez un user dans RH pour pouvoir assigner.
                </p>
              )}

              {editing && (
                <form
                  onSubmit={onSaveEdit}
                  className="grid gap-2 border-b border-border bg-muted/30 p-4 sm:grid-cols-2"
                >
                  <div className="flex items-center justify-between gap-2 sm:col-span-2">
                    <p className="text-sm font-medium">Modifier la tâche</p>
                    <button
                      type="button"
                      className="inline-flex size-8 items-center justify-center rounded-md hover:bg-accent"
                      onClick={() => setEditing(null)}
                      aria-label="Fermer"
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                  <input
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    className="flex h-9 rounded-md border border-input bg-background px-3 text-sm sm:col-span-2"
                    required
                  />
                  <select
                    value={editStatus}
                    onChange={(e) => setEditStatus(e.target.value as TaskStatus)}
                    className="flex h-9 rounded-md border border-input bg-background px-3 text-sm"
                  >
                    {STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {statusLabel(s)}
                      </option>
                    ))}
                  </select>
                  <select
                    value={editAssignee}
                    onChange={(e) => setEditAssignee(e.target.value)}
                    className="flex h-9 rounded-md border border-input bg-background px-3 text-sm"
                  >
                    <option value="">Non assignée</option>
                    {employes.map((emp) => (
                      <option key={emp.id} value={emp.id}>
                        {emp.nom}
                      </option>
                    ))}
                  </select>
                  <div className="flex gap-2 sm:col-span-2">
                    <button
                      type="submit"
                      disabled={busy}
                      className="inline-flex h-9 items-center rounded-md bg-primary px-3 text-sm text-primary-foreground"
                    >
                      Enregistrer
                    </button>
                    <button
                      type="button"
                      className="inline-flex h-9 items-center rounded-md border border-input px-3 text-sm"
                      onClick={() => setEditing(null)}
                    >
                      Annuler
                    </button>
                  </div>
                </form>
              )}

              <div className="min-h-0 flex-1 overflow-y-auto">
                {tasks.length === 0 && (
                  <div className="px-5 py-12 text-center text-sm text-muted-foreground">
                    Aucune tâche dans ce projet.
                  </div>
                )}
                {tasks.map((task) => (
                  <div
                    key={task.id}
                    className="flex items-start justify-between gap-3 border-b border-border px-4 py-3 last:border-b-0"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium">{task.title}</p>
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        {statusLabel(task.status)}
                        {task.assigneeName ? ` · ${task.assigneeName}` : ' · Non assignée'}
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      <button
                        type="button"
                        className="inline-flex size-8 items-center justify-center rounded-md hover:bg-accent"
                        onClick={() => openEdit(task)}
                        aria-label="Modifier"
                      >
                        <Pencil className="size-3.5" />
                      </button>
                      <button
                        type="button"
                        className="inline-flex size-8 items-center justify-center rounded-md text-destructive hover:bg-accent"
                        onClick={() => void onDeleteTask(task.id)}
                        aria-label="Supprimer"
                      >
                        <Trash2 className="size-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
