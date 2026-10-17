// Small wrapper around fetch: adds the JWT, parses JSON and turns error responses into exceptions.

const TOKEN_KEY = 'ledgerbank.token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  const token = tokenStore.get();
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running on port 8080?', 0);
  }

  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      // Token expired or invalid: log the user out everywhere in the app
      window.dispatchEvent(new Event('auth:expired'));
    }
    throw new ApiError(data?.message || `Request failed (${response.status})`, response.status, data?.fieldErrors);
  }
  return data;
}

/** A unique reference per transfer. Re-sending the same one never moves money twice. */
export function newReference(prefix = 'TXN') {
  const id = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${id}`;
}
