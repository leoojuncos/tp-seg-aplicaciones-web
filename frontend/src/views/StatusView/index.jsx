import { useCallback, useEffect, useState } from 'react';
import { getJson } from '../../api/http.js';
import { Message, SectionHeading, Table } from '../../components/common/index.js';

const BACKENDS = [
  { key: 'monolith', name: 'Monolito', path: '/api/health' },
  { key: 'auditoria', name: 'Auditoría', path: '/auditoria/api/health' },
];

const COLUMNS = [
  { key: 'name', header: 'Servicio' },
  { key: 'path', header: 'Ruta', render: (row) => <code>{row.path}</code> },
  { key: 'status', header: 'Estado', render: (row) => <StatusLabel result={row.result} /> },
  { key: 'db', header: 'Base de datos', render: (row) => row.result?.body?.db ?? '—' },
  { key: 'rabbitmq', header: 'RabbitMQ', render: (row) => row.result?.body?.rabbitmq ?? '—' },
  { key: 'http', header: 'HTTP', align: 'end', render: (row) => row.result?.status ?? '—' },
];

// Estado de los dos backends. Es el ejemplo de pantalla de listado: SectionHeading con la accion,
// Message y Table.
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

  const rows = BACKENDS.map((backend) => ({ ...backend, result: results[backend.key] }));
  const checking = rows.some((row) => !row.result);
  const failing = rows.filter((row) => row.result && row.result.body?.status !== 'UP');

  return (
    <>
      <SectionHeading
        titles={['Estado de los servicios']}
        actions={
          <button type="button" className="btn btn-primary" onClick={refresh} disabled={checking}>
            Actualizar
          </button>
        }
      />
      <section className="section-panel">
        {!checking &&
          (failing.length > 0 ? (
            <Message type="error">
              Con problemas: {failing.map((row) => row.name).join(' y ')}. Revisá los contenedores con{' '}
              <code>docker compose ps</code>.
            </Message>
          ) : (
            <Message type="success">Los dos backends responden.</Message>
          ))}
        <Table columns={COLUMNS} rows={rows} rowKey="key" />
      </section>
    </>
  );
}

function StatusLabel({ result }) {
  // Sin resultado todavia: consultando. Con respuesta JSON: el status que informa el backend (UP o
  // DOWN, con 200 o 503). Sin JSON: el proxy no llego al backend.
  const label = !result ? 'consultando…' : (result.body?.status ?? 'sin respuesta');
  return <strong data-status={label}>{label}</strong>;
}
