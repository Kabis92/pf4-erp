export type EmployeStatut = 'ACTIF' | 'CONGE' | 'INACTIF';

export type Employe = {
  id: string;
  nom: string;
  email: string;
  telephone: string | null;
  poste: string;
  dateEmbauche: string;
  userId: string | null;
  statut: EmployeStatut;
  hasAccess: boolean;
};

export type EmployeInput = {
  nom: string;
  email: string;
  telephone: string | null;
  poste: string;
  dateEmbauche: string;
  userId: string | null;
  statut: EmployeStatut;
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

export function setEmployeAccess(id: string, enabled: boolean, userId?: string | null) {
  return request<Employe>(`/api/rh/employes/${id}/access`, {
    method: 'PUT',
    body: JSON.stringify({ enabled, userId: userId ?? null }),
  });
}

export function deleteEmploye(id: string) {
  return request<void>(`/api/rh/employes/${id}`, {
    method: 'DELETE',
  });
}

export function statutLabel(statut: EmployeStatut) {
  switch (statut) {
    case 'ACTIF':
      return 'Active';
    case 'CONGE':
      return 'On Leave';
    case 'INACTIF':
      return 'Inactive';
    default:
      return statut;
  }
}

export function formatJoiningDate(iso: string) {
  const d = new Date(`${iso}T00:00:00`);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}
