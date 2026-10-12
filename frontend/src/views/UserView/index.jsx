import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { errorMessage, getJson } from '../../api/http.js';
import { BackButton, Field, Message, SectionHeading } from '../../components/common/index.js';
import { formatRole } from '../../utils/format.js';

const CONNECTION_ERROR = 'No se pudo conectar con el servidor.';

// Detalle de un usuario: su rol y los codigos de permiso que tiene. Solo lectura: SectionHeading con
// la miga al listado, los datos en Field deshabilitados y Volver al pie.
export default function UserView() {
  const { id } = useParams();
  const [user, setUser] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      const response = await getJson(`/api/administracion/users/${id}`);
      if (response.ok) {
        setUser(response.body);
      } else {
        setError(errorMessage(response));
      }
    } catch {
      setError(CONNECTION_ERROR);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <>
      <SectionHeading titles={[{ title: 'Usuarios', url: '/administracion/usuarios' }, 'Detalle']} />
      <section className="section-panel">
        <Message type="error">{error}</Message>
        {user && (
          <div className="row">
            <div className="col-md-4">
              <Field label="Usuario" name="username" value={user.username} disabled />
            </div>
            <div className="col-md-4">
              <Field label="Rol" name="role" value={formatRole(user.role)} disabled />
            </div>
            <div className="col-md-4">
              <Field
                label="Permisos"
                name="permissions"
                value={user.permissions.length > 0 ? user.permissions.join(', ') : '—'}
                disabled
              />
            </div>
          </div>
        )}
        <div className="section-actions">
          <BackButton to="/administracion/usuarios" />
        </div>
      </section>
    </>
  );
}
