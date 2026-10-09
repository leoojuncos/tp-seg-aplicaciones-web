import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage, getJson } from '../../api/http.js';
import { Field, Message, SectionHeading, Table } from '../../components/common/index.js';
import { useForm } from '../../hooks/useForm.js';
import { formatAmount, formatCuit, formatDebtStatus } from '../../utils/format.js';

// Deudas de Tesoreria con busqueda por CUIT. Es la entrada del modulo y el ejemplo de listado con
// filtro: SectionHeading, el filtro arriba, Message y Table, con la accion Ver de cada fila.
export default function DebtsView() {
  const navigate = useNavigate();
  const [filters, handleChange] = useForm({ cuit: '' });
  const [cuitError, setCuitError] = useState(null);
  const [result, setResult] = useState({ debts: [], loading: true, error: null });

  const search = useCallback(async (cuit) => {
    setResult((previous) => ({ ...previous, loading: true, error: null }));
    try {
      const response = await getJson(cuit ? `/api/tesoreria/debts?cuit=${cuit}` : '/api/tesoreria/debts');
      setResult(
        response.ok
          ? { debts: response.body, loading: false, error: null }
          : { debts: [], loading: false, error: errorMessage(response) },
      );
    } catch {
      setResult({ debts: [], loading: false, error: 'No se pudo conectar con el servidor.' });
    }
  }, []);

  useEffect(() => {
    search('');
  }, [search]);

  // El CUIT va con sus 11 digitos, sin guiones, como lo recibe la API; vacio, trae todas. Mientras hay
  // una busqueda en curso no arranca otra: Enter no pasa por el disabled del boton.
  const submit = () => {
    if (result.loading) {
      return;
    }
    const cuit = filters.cuit.trim();
    if (cuit && !/^\d{11}$/.test(cuit)) {
      setCuitError('Ingresá los 11 dígitos del CUIT, sin guiones.');
      return;
    }
    setCuitError(null);
    search(cuit);
  };

  const columns = [
    { key: 'cuit', header: 'CUIT', render: (row) => formatCuit(row.cuit) },
    { key: 'amount', header: 'Importe', align: 'end', render: (row) => formatAmount(row.amount) },
    { key: 'status', header: 'Estado', render: (row) => formatDebtStatus(row.status) },
    {
      key: 'actions',
      header: '',
      align: 'end',
      width: '1%',
      render: (row) => (
        <button
          type="button"
          className="btn-icon"
          title="Ver"
          aria-label={`Ver la deuda del CUIT ${formatCuit(row.cuit)}`}
          onClick={() => navigate(`/tesoreria/deuda/${row.id}`)}
        >
          <span className="material-symbols-outlined" aria-hidden="true">
            search
          </span>
        </button>
      ),
    },
  ];

  return (
    <>
      <SectionHeading titles={['Deudas']} />
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
        <Table columns={columns} rows={result.debts} loading={result.loading} />
      </section>
    </>
  );
}
