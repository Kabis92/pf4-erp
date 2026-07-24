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
  title: string;
  description: string;
  status: TaskStatus;
  assigneeEmployeId: string | null;
  assigneeName: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
};

export type EmployeOption = {
  id: string;
  nom: string;
  userId: string | null;
};

function getApi() {
  return window.__PF4ERP__;
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

export function deleteTask(id: string) {
  return request<void>(`/api/todo/tasks/${id}`, { method: 'DELETE' });
}

/** Employés pouvant recevoir une tâche = liés à un compte utilisateur. */
export async function listEmployesForAssign() {
  const all = await request<EmployeOption[]>('/api/rh/employes');
  return all.filter((e) => Boolean(e.userId));
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
