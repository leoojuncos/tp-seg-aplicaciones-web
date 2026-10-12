import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage, getJson } from '../../api/http.js';
import { Field, Message, SectionHeading, Select, Table } from '../../components/common/index.js';
import { useForm } from '../../hooks/useForm.js';
import { Modules } from '../../routes/modules.js';
import { formatCuit, formatDateTime, formatEventType } from '../../utils/format.js';

// El selector lista todos los modulos, incluido Tesoreria, aunque la sesion no lo tenga habilitado:
// es la pista del paso 2 (desde Auditoria se descubre que existe Tesoreria).
const MODULE_OPTIONS = Modules.map((module) => ({ value: module.code, label: module.title }));

// Arma el query string del GET con los filtros que tienen valor. Todos son query params opcionales
// que resuelve Auditoria: el modulo, el CUIT y el rango por fecha de registro (from/to, YYYY-MM-DD).
function queryFor({ module, cuit, from, to }) {
  const params = new URLSearchParams();
  if (module) params.set('module', module);
  if (cuit) params.set('cuit', cuit);
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString();
  return query ? `?${query}` : '';
}

// Eventos de auditoria registrados. Es la entrada del modulo y un listado con filtros como DebtsView:
// SectionHeading, los filtros arriba, Message y Table, con la accion Ver de cada fila.
export default function EventsView() {
  const navigate = useNavigate();
  const [filters, handleChange] = useForm({ module: '', cuit: '', from: '', to: '' });
  const [cuitError, setCuitError] = useState(null);
  const [result, setResult] = useState({ events: [], loading: true, error: null });

  const search = useCallback(async (applied) => {
    setResult((previous) => ({ ...previous, loading: true, error: null }));
    try {
      const response = await getJson(`/auditoria/api/events${queryFor(applied)}`);
      setResult(
        response.ok
          ? { events: response.body, loading: false, error: null }
          : { events: [], loading: false, error: errorMessage(response) },
      );
    } catch {
      setResult({ events: [], loading: false, error: 'No se pudo conectar con el servidor.' });
    }
  }, []);

  useEffect(() => {
    search({});
  }, [search]);

  // El CUIT va con sus 11 digitos, sin guiones, como lo recibe la API; vacio, no filtra. Mientras hay
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
    search({ ...filters, cuit });
  };

  const columns = [
    { key: 'type', header: 'Tipo', render: (row) => formatEventType(row.type) },
    { key: 'cuit', header: 'CUIT', render: (row) => formatCuit(row.cuit) },
    { key: 'registeredAt', header: 'Registrado', render: (row) => formatDateTime(row.registeredAt) },
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
          aria-label={`Ver el evento del CUIT ${formatCuit(row.cuit)}`}
          onClick={() => navigate(`/auditoria/evento/${row.id}`)}
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
      <SectionHeading titles={['Eventos']} />
      <section className="section-panel">
        {/* Desde/Hasta filtran por la fecha de registro, la columna Registrado. */}
        <div className="row gx-2 align-items-start">
          <div className="col-sm-6 col-lg-3">
            <Select
              label="Módulo"
              name="module"
              value={filters.module}
              onChange={handleChange}
              options={MODULE_OPTIONS}
              emptyOption="Todos"
            />
          </div>
          <div className="col-sm-6 col-lg-3">
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
          <div className="col-sm-6 col-lg-2">
            <Field label="Desde" name="from" type="date" value={filters.from} onChange={handleChange} onEnter={submit} />
          </div>
          <div className="col-sm-6 col-lg-2">
            <Field label="Hasta" name="to" type="date" value={filters.to} onChange={handleChange} onEnter={submit} />
          </div>
          {/* mt-4: la altura de la etiqueta del campo, para alinear el boton con los inputs. */}
          <div className="col-auto mt-4">
            <button type="button" className="btn btn-primary" onClick={submit} disabled={result.loading}>
              Buscar
            </button>
          </div>
        </div>
        <Message type="error">{result.error}</Message>
        <Table columns={columns} rows={result.events} loading={result.loading} />
      </section>
    </>
  );
}
