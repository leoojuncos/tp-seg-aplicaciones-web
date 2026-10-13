import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { errorMessage, getJson } from '../../api/http.js';
import { BackButton, Field, Message, SectionHeading } from '../../components/common/index.js';
import { formatCuit, formatDateTime, formatEventType } from '../../utils/format.js';

const CONNECTION_ERROR = 'No se pudo conectar con el servidor.';

// Detalle de un evento de auditoria registrado. Solo lectura: SectionHeading con la miga al listado,
// los datos en Field deshabilitados y Volver al pie.
export default function EventView() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      const response = await getJson(`/auditoria/api/events/${id}`);
      if (response.ok) {
        setEvent(response.body);
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
      <SectionHeading titles={[{ title: 'Eventos', url: '/auditoria/eventos' }, 'Detalle']} />
      <section className="section-panel">
        <Message type="error">{error}</Message>
        {event && (
          <div className="row">
            <div className="col-md-3">
              <Field label="Tipo" name="type" value={formatEventType(event.type)} disabled />
            </div>
            <div className="col-md-3">
              <Field label="CUIT" name="cuit" value={formatCuit(event.cuit)} disabled />
            </div>
            <div className="col-md-3">
              <Field label="Ocurrió" name="occurredAt" value={formatDateTime(event.occurredAt)} disabled />
            </div>
            <div className="col-md-3">
              <Field label="Registrado" name="registeredAt" value={formatDateTime(event.registeredAt)} disabled />
            </div>
          </div>
        )}
        <div className="section-actions">
          <BackButton to="/auditoria/eventos" />
        </div>
      </section>
    </>
  );
}
