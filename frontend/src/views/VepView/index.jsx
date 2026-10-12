import { useState } from 'react';
import { errorMessage, getJson } from '../../api/http.js';
import { Field, Message, SectionHeading, Table } from '../../components/common/index.js';
import { useForm } from '../../hooks/useForm.js';
import { formatAmount, formatCuit, formatDebtStatus } from '../../utils/format.js';

const COLUMNS = [
  { key: 'cuit', header: 'CUIT', render: (row) => formatCuit(row.cuit) },
  { key: 'amount', header: 'Importe', align: 'end', render: (row) => formatAmount(row.amount) },
  { key: 'status', header: 'Estado', render: (row) => formatDebtStatus(row.status) },
];

// Consulta publica de deuda (VEP), sin sesion. El filtro es el de DebtsView, pero el CUIT es obligatorio
// y no se muestra nada hasta buscar: la deuda del CUIT va en una Table de una fila, con su estado actual
// (una condonada conserva su importe original). Si el CUIT no tiene deuda, la API responde 404 y la
// tabla queda vacia.
export default function VepView() {
  const [filters, handleChange] = useForm({ cuit: '' });
  const [cuitError, setCuitError] = useState(null);
  // cuit es el ultimo consultado: hasta la primera busqueda es null y no hay tabla.
  const [result, setResult] = useState({ cuit: null, debts: [], loading: false, error: null });

  const search = async (cuit) => {
    setResult({ cuit, debts: [], loading: true, error: null });
    try {
      const response = await getJson(`/api/vep/debts/${cuit}`);
      if (response.ok) {
        setResult({ cuit, debts: [response.body], loading: false, error: null });
      } else {
        const error = response.status === 404 ? null : errorMessage(response);
        setResult({ cuit, debts: [], loading: false, error });
      }
    } catch {
      setResult({ cuit, debts: [], loading: false, error: 'No se pudo conectar con el servidor.' });
    }
  };

  // El CUIT va con sus 11 digitos, sin guiones, como lo recibe la API. Mientras hay una busqueda en
  // curso no arranca otra: Enter no pasa por el disabled del boton.
  const submit = () => {
    if (result.loading) {
      return;
    }
    const cuit = filters.cuit.trim();
    if (!/^\d{11}$/.test(cuit)) {
      setCuitError('Ingresá los 11 dígitos del CUIT, sin guiones.');
      return;
    }
    setCuitError(null);
    search(cuit);
  };

  return (
    <>
      <SectionHeading titles={['Consulta de deuda']} />
      <section className="section-panel">
        <div className="row gx-2 align-items-start">
          <div className="col-sm-6 col-lg-4">
            <Field
              label="CUIT"
              name="cuit"
              value={filters.cuit}
              onChange={handleChange}
              onEnter={submit}
              error={cuitError}
              placeholder="20123456789"
            />
          </div>
          {/* mt-4: la altura de la etiqueta del campo, para alinear el boton con el input. */}
          <div className="col-auto mt-4">
            <button type="button" className="btn btn-primary" onClick={submit} disabled={result.loading}>
              Buscar
            </button>
          </div>
        </div>
        <Message type="error">{result.error}</Message>
        {result.cuit && !result.error && (
          <Table
            columns={COLUMNS}
            rows={result.debts}
            rowKey="cuit"
            loading={result.loading}
            emptyMessage={`El CUIT ${formatCuit(result.cuit)} no registra deuda.`}
          />
        )}
      </section>
    </>
  );
}
