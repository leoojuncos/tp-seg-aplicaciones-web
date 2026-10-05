import { useCallback, useEffect, useState } from 'react';
import { getJson } from '../../api/http.js';

const BACKENDS = [
  { key: 'monolith', name: 'Monolito', path: '/api/health' },
  { key: 'auditoria', name: 'Auditoría', path: '/auditoria/api/health' },
];

export default function StatusView() {
  const [results, setResults] = useState({});

  const refresh = useCallback(async () => {
    setResults({});
    const entries = await Promise.all(
      BACKENDS.map(async (backend) => {
        try {
          return [backend.key, await getJson(backend.path)];
        } catch {
          // Fallo de red: ni siquiera hubo respuesta del proxy.
          return [backend.key, { ok: false, status: null, body: null }];
        }
      }),
    );
    setResults(Object.fromEntries(entries));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <section>
      <h1>Estado de los servicios</h1>
      <button type="button" onClick={refresh}>
        Actualizar
      </button>
      <div className="cards">
        {BACKENDS.map((backend) => (
          <BackendCard key={backend.key} backend={backend} result={results[backend.key]} />
        ))}
      </div>
    </section>
  );
}

function BackendCard({ backend, result }) {
  const body = result?.body;
  // Sin resultado todavia: consultando. Con respuesta JSON: el status que informa el backend
  // (UP o DOWN, con 200 o 503). Sin JSON: el proxy no llego al backend.
  const label = !result ? 'consultando…' : (body?.status ?? 'sin respuesta');
  return (
    <article className="card">
      <h2>{backend.name}</h2>
      <p>
        <code>{backend.path}</code> · <strong data-status={label}>{label}</strong>
        {result?.status ? ` (HTTP ${result.status})` : ''}
      </p>
      {body && (
        <ul>
          <li>Base de datos: {body.db}</li>
          <li>RabbitMQ: {body.rabbitmq}</li>
        </ul>
      )}
    </article>
  );
}
