import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage, getJson } from '../../api/http.js';
import { Message, SectionHeading, Table } from '../../components/common/index.js';
import { formatRole } from '../../utils/format.js';

// Usuarios del SGM y su rol. Es la entrada del modulo de Administracion y un listado de solo lectura:
// SectionHeading, Message y Table, con la accion Ver de cada fila.
export default function UsersView() {
  const navigate = useNavigate();
  const [result, setResult] = useState({ users: [], loading: true, error: null });

  const load = useCallback(async () => {
    try {
      const response = await getJson('/api/administracion/users');
      setResult(
        response.ok
          ? { users: response.body, loading: false, error: null }
          : { users: [], loading: false, error: errorMessage(response) },
      );
    } catch {
      setResult({ users: [], loading: false, error: 'No se pudo conectar con el servidor.' });
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const columns = [
    { key: 'username', header: 'Usuario' },
    { key: 'role', header: 'Rol', render: (row) => formatRole(row.role) },
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
          aria-label={`Ver el usuario ${row.username}`}
          onClick={() => navigate(`/administracion/usuario/${row.id}`)}
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
      <SectionHeading titles={['Usuarios']} />
      <section className="section-panel">
        <Message type="error">{result.error}</Message>
        <Table columns={columns} rows={result.users} loading={result.loading} />
      </section>
    </>
  );
}
