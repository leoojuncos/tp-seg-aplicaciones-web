// Las rutas son relativas al origen del front: el proxy de Vite lleva /api al monolito y
// /auditoria/api a Auditoria, y con ellas viajan la cookie de sesion y el header Authorization.

let authHandlers = {};

// Los registra SessionProvider: que hacer ante un 401 (sin sesion) y ante un 403 (sin permiso).
export function setAuthHandlers(handlers) {
  authHandlers = handlers;
}

// Devuelve { ok, status, body }; body es el JSON de la respuesta o null. Con authRedirect (el
// default), un 401 limpia la sesion y manda al login, y un 403 manda a /unauthorized. Un fallo de red
// rechaza la promesa, como fetch.
export async function request(method, path, body, { authRedirect = true } = {}) {
  const headers = { Accept: 'application/json' };
  const init = { method, headers };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }
  const response = await fetch(path, init);
  const isJson = (response.headers.get('content-type') ?? '').includes('application/json');
  const text = await response.text();
  const result = { ok: response.ok, status: response.status, body: isJson && text ? JSON.parse(text) : null };
  if (authRedirect && response.status === 401) authHandlers.onUnauthorized?.();
  if (authRedirect && response.status === 403) authHandlers.onForbidden?.();
  return result;
}

export const getJson = (path, options) => request('GET', path, undefined, options);
export const postJson = (path, body, options) => request('POST', path, body, options);
export const putJson = (path, body, options) => request('PUT', path, body, options);
export const deleteJson = (path, options) => request('DELETE', path, undefined, options);

// El message del formato de error del SGM, o uno generico si la respuesta no lo trae.
export function errorMessage(response, fallback = 'No se pudo completar la operación.') {
  return response?.body?.message ?? fallback;
}
