export type Employe = {
  id: string;
  nom: string;
  email: string;
  poste: string;
  dateEmbauche: string;
  userId: string | null;
};

export type EmployeInput = {
  nom: string;
  email: string;
  poste: string;
  dateEmbauche: string;
  userId: string | null;
};

export type UserOption = {
  id: string;
  username: string;
  displayName: string;
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

export function listEmployes() {
  return request<Employe[]>('/api/rh/employes');
}

export function countEmployes() {
  return request<{ count: number }>('/api/rh/employes/count');
}

export function listDirectoryUsers() {
  return request<UserOption[]>('/api/directory/users');
}

export function createEmploye(input: EmployeInput) {
  return request<Employe>('/api/rh/employes', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateEmploye(id: string, input: EmployeInput) {
  return request<Employe>(`/api/rh/employes/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function deleteEmploye(id: string) {
  return request<void>(`/api/rh/employes/${id}`, {
    method: 'DELETE',
  });
}
