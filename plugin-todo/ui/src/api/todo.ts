export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export type Project = {
  id: string;
  name: string;
  description: string;
  createdByUserId: string;
  createdAt: string;
};

export type Task = {
  id: string;
  projectId: string;
  projectName?: string;
  title: string;
  description: string;
  status: TaskStatus;
  assigneeEmployeId: string | null;
  assigneeName: string | null;
  favorite: boolean;
  deleted: boolean;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
};

export type EmployeOption = {
  id: string;
  nom: string;
};

export function listEmployesForAssign() {
  return request<EmployeOption[]>('/api/todo/assignees');
}

function getApi() {
  return window.__PF4ERP__;
}

function hasPermission(code: string) {
  const perms = getApi()?.getPermissions?.() ?? [];
  return perms.includes(code);
}

export function canManageTodo() {
  return hasPermission('todo.project.manage');
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const api = getApi();
  if (!api) {
    throw new Error('API runtime indisponible');
  }
  const token = api.getAuthToken();
  const res = await fetch(`${api.apiBaseUrl}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });
  if (!res.ok) {
    let detail = `HTTP ${res.status}`;
    try {
      const body = (await res.json()) as { message?: string };
      if (body.message) {
        detail = body.message;
      }
    } catch {
      // ignore
    }
    throw new Error(detail);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

export function listMyTasks() {
  return request<Task[]>('/api/todo/tasks/mine');
}

export function updateMyTaskStatus(id: string, status: TaskStatus) {
  return request<Task>(`/api/todo/tasks/mine/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  });
}

export function setMyFavorite(id: string, favorite: boolean) {
  return request<Task>(`/api/todo/tasks/mine/${id}/favorite`, {
    method: 'PUT',
    body: JSON.stringify({ favorite }),
  });
}

export function listProjects() {
  return request<Project[]>('/api/todo/projects');
}

export function createProject(input: { name: string; description?: string }) {
  return request<Project>('/api/todo/projects', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function deleteProject(id: string) {
  return request<void>(`/api/todo/projects/${id}`, { method: 'DELETE' });
}

export function listTasks(projectId?: string) {
  const q = projectId ? `?projectId=${encodeURIComponent(projectId)}` : '';
  return request<Task[]>(`/api/todo/tasks${q}`);
}

export function createTask(input: {
  projectId: string;
  title: string;
  description?: string;
  assigneeEmployeId?: string | null;
}) {
  return request<Task>('/api/todo/tasks', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateTask(
  id: string,
  input: { title: string; description?: string; assigneeEmployeId?: string | null },
) {
  return request<Task>(`/api/todo/tasks/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function updateTaskStatus(id: string, status: TaskStatus) {
  return request<Task>(`/api/todo/tasks/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  });
}

export function setTaskFavorite(id: string, favorite: boolean) {
  return request<Task>(`/api/todo/tasks/${id}/favorite`, {
    method: 'PUT',
    body: JSON.stringify({ favorite }),
  });
}

export function trashTask(id: string) {
  return request<Task>(`/api/todo/tasks/${id}/trash`, { method: 'PUT' });
}

export function restoreTask(id: string) {
  return request<Task>(`/api/todo/tasks/${id}/restore`, { method: 'PUT' });
}

export function deleteTask(id: string) {
  return request<void>(`/api/todo/tasks/${id}`, { method: 'DELETE' });
}

export function statusLabel(status: TaskStatus) {
  switch (status) {
    case 'TODO':
      return 'À faire';
    case 'IN_PROGRESS':
      return 'En cours';
    case 'DONE':
      return 'Terminée';
    default:
      return status;
  }
}

/** Couleurs de tags projet (pas de thème violet global — accents locaux). */
export function projectTagClass(name: string) {
  const palette = [
    'bg-rose-500/15 text-rose-700 dark:text-rose-300',
    'bg-orange-500/15 text-orange-700 dark:text-orange-300',
    'bg-teal-500/15 text-teal-700 dark:text-teal-300',
    'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300',
    'bg-sky-500/15 text-sky-700 dark:text-sky-300',
    'bg-amber-500/15 text-amber-800 dark:text-amber-300',
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash + name.charCodeAt(i) * (i + 1)) % palette.length;
  }
  return palette[hash];
}

export function projectBorderClass(name: string) {
  const palette = [
    'border-l-rose-400',
    'border-l-orange-400',
    'border-l-teal-400',
    'border-l-emerald-400',
    'border-l-sky-400',
    'border-l-amber-400',
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash + name.charCodeAt(i) * (i + 1)) % palette.length;
  }
  return palette[hash];
}
