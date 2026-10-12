import { useCallback, useEffect, useState } from 'react';
import { errorMessage, getJson } from '../../api/http.js';
import { Message, SectionHeading, Table } from '../../components/common/index.js';
import './index.css';

const INTEGRATIONS = [
  { key: 'database', name: 'Base de datos' },
  { key: 'rabbitmq', name: 'RabbitMQ' },
  { key: 'messaging', name: 'Módulo de mensajería' },
];

const COLUMNS = [
  { key: 'name', header: 'Integración' },
  { key: 'status', header: 'Estado', render: (row) => <strong data-status={row.status}>{row.status}</strong> },
];

const TIME = new Intl.DateTimeFormat('es-AR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });

// Diagnostico de integraciones de Tesoreria: el estado de la base, RabbitMQ y el modulo de mensajeria,
// como StatusView (SectionHeading con Actualizar, Message y Table). La respuesta del endpoint trae ademas
// la configuracion del modulo (URL y credencial de su cuenta tecnica, en Base64): es el paso 3 del
// escenario (ver AGENTS.md), visible en la pestaña Network. La pantalla no la muestra.
export default function DiagnosticsView() {
  const [result, setResult] = useState({ diagnostics: null, loading: true, error: null, checkedAt: null });

  const load = useCallback(async () => {
    setResult((previous) => ({ ...previous, loading: true, error: null }));
    try {
      const response = await getJson('/api/tesoreria/diagnostics');
      setResult(
        response.ok
          ? { diagnostics: response.body, loading: false, error: null, checkedAt: new Date() }
          : { diagnostics: null, loading: false, error: errorMessage(response), checkedAt: null },
      );
    } catch {
      setResult({ diagnostics: null, loading: false, error: 'No se pudo conectar con el servidor.', checkedAt: null });
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const { diagnostics, loading, error, checkedAt } = result;
  const rows = diagnostics
    ? INTEGRATIONS.map((integration) => ({ ...integration, status: diagnostics[integration.key]?.status }))
    : [];
  const failing = rows.filter((row) => row.status !== 'UP');

  return (
    <>
      <SectionHeading
        titles={['Diagnóstico de integraciones']}
        actions={
          <>
            {checkedAt && <span className="diagnostics-checked">Última verificación {TIME.format(checkedAt)}</span>}
            <button type="button" className="btn btn-primary" onClick={load} disabled={loading}>
              Actualizar
            </button>
          </>
        }
      />
      <section className="section-panel">
        <Message type="error">{error}</Message>
        {diagnostics &&
          !loading &&
          (failing.length > 0 ? (
            <Message type="error">Con problemas: {failing.map((row) => row.name).join(', ')}.</Message>
          ) : (
            <Message type="success">Las tres integraciones responden.</Message>
          ))}
        {(loading || diagnostics) && <Table columns={COLUMNS} rows={rows} rowKey="key" loading={loading} />}
      </section>
    </>
  );
}
