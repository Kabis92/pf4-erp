import { getToken } from '@/lib/auth';

export async function adminFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  const hasBody = init?.body != null && !(init.body instanceof FormData);
  if (hasBody) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(path, {
    ...init,
    headers: {
      ...headers,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const body = (await res.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore
    }
    throw new Error(message);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}
