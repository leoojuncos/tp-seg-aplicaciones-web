import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage, getJson } from '../../api/http.js';
import { Message, Table } from '../../components/common/index.js';
import { usePortalSession } from '../../hooks/usePortalSession.js';
import { formatAmount, formatCuit, formatDebtStatus } from '../../utils/format.js';
import './index.css';

// Accesos del inicio del portal: todos decorativos (placeholder en routes/portal.js).
const SHORTCUTS = [
  { path: '/pagos', title: 'Pagar en línea', icon: 'payments' },
  { path: '/planes-de-pago', title: 'Planes de pago', icon: 'calendar_month' },
  { path: '/libre-deuda', title: 'Libre deuda', icon: 'verified' },
  { path: '/boleta-electronica', title: 'Boleta electrónica', icon: 'mail' },
];

const STATUS_ICONS = { PENDING: 'error', FORGIVEN: 'check_circle' };

const COLUMNS = [
  { key: 'cuit', header: 'CUIT', render: (row) => formatCuit(row.cuit) },
  { key: 'amount', header: 'Importe', align: 'end', render: (row) => `$ ${formatAmount(row.amount)}` },
  { key: 'status', header: 'Estado', render: (row) => <DebtStatus status={row.status} /> },
];

// Inicio del portal del contribuyente: los accesos (decorativos) y sus deudas con su estado actual (una
// condonada conserva su importe original). No hay buscador: muestra solo las del CUIT con el que se
// ingreso. Si no tiene deuda, la API responde 404 y la tabla queda vacia.
export default function VepView() {
  const { taxpayer } = usePortalSession();
  const [result, setResult] = useState({ debts: [], loading: true, error: null });

  const load = useCallback(async () => {
    try {
      const response = await getJson(`/api/vep/debts/${taxpayer.cuit}`);
      if (response.ok) {
        setResult({ debts: [response.body], loading: false, error: null });
      } else {
        const error = response.status === 404 ? null : errorMessage(response);
        setResult({ debts: [], loading: false, error });
      }
    } catch {
      setResult({ debts: [], loading: false, error: 'No se pudo conectar con el servidor.' });
    }
  }, [taxpayer.cuit]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <>
      <section className="vep-shortcuts">
        {SHORTCUTS.map((shortcut) => (
          <Link key={shortcut.path} to={shortcut.path} className="vep-shortcut">
            <span className="vep-shortcut-icon">
              <span className="material-symbols-outlined" aria-hidden="true">
                {shortcut.icon}
              </span>
            </span>
            {shortcut.title}
          </Link>
        ))}
      </section>
      <section className="vep-debts">
        <h2>
          <span className="material-symbols-outlined" aria-hidden="true">
            receipt_long
          </span>
          Mis deudas
        </h2>
        <Message type="error">{result.error}</Message>
        {!result.error && (
          <Table
            columns={COLUMNS}
            rows={result.debts}
            rowKey="cuit"
            loading={result.loading}
            emptyMessage="No registrás deudas."
          />
        )}
      </section>
    </>
  );
}

function DebtStatus({ status }) {
  return (
    <strong className="vep-debt-status" data-debt-status={status}>
      {formatDebtStatus(status)}
      <span className="material-symbols-outlined" aria-hidden="true">
        {STATUS_ICONS[status]}
      </span>
    </strong>
  );
}
