// Las rutas son relativas al origen del front: el proxy de Vite lleva /api al monolito y
// /auditoria/api a Auditoria, y con ellas viajan la cookie de sesion y el header Authorization.
export async function getJson(path) {
  const response = await fetch(path, { headers: { Accept: 'application/json' } });
  const isJson = (response.headers.get('content-type') ?? '').includes('application/json');
  return {
    ok: response.ok,
    status: response.status,
    body: isJson ? await response.json() : null,
  };
}
