import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { errorMessage, getJson, postJson } from '../../api/http.js';
import { BackButton, ConfirmModal, Field, Message, SectionHeading } from '../../components/common/index.js';
import { formatAmount, formatCuit, formatDebtStatus } from '../../utils/format.js';

const CONNECTION_ERROR = 'No se pudo conectar con el servidor.';

// Detalle de una deuda y su condonacion. Es el ejemplo de detalle: SectionHeading con la miga al
// listado, los datos en Field deshabilitados y, al pie, Volver y la accion, que pide confirmacion con
// ConfirmModal. El resultado se muestra con Message.
export default function DebtView() {
  const { id } = useParams();
  const [debt, setDebt] = useState(null);
  const [message, setMessage] = useState(null);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const response = await getJson(`/api/tesoreria/debts/${id}`);
      if (response.ok) {
        setDebt(response.body);
      } else {
        setMessage({ type: 'error', text: errorMessage(response) });
      }
    } catch {
      setMessage({ type: 'error', text: CONNECTION_ERROR });
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const forgive = async () => {
    setBusy(true);
    try {
      const response = await postJson(`/api/tesoreria/debts/${id}/forgive`);
      if (response.ok) {
        setDebt(response.body);
        setMessage({ type: 'success', text: 'La deuda quedó condonada.' });
      } else {
        setMessage({ type: 'error', text: errorMessage(response) });
        // 409: la condonaron mientras estaba abierta esta pantalla; se vuelve a leer su estado.
        if (response.status === 409) {
          await load();
        }
      }
    } catch {
      setMessage({ type: 'error', text: CONNECTION_ERROR });
    } finally {
      setBusy(false);
      setConfirming(false);
    }
  };

  return (
    <>
      <SectionHeading titles={[{ title: 'Deudas', url: '/tesoreria/deudas' }, 'Detalle']} />
      <section className="section-panel">
        <Message type={message?.type}>{message?.text}</Message>
        {debt && (
          <div className="row">
            <div className="col-md-4">
              <Field label="CUIT" name="cuit" value={formatCuit(debt.cuit)} disabled />
            </div>
            <div className="col-md-4">
              <Field label="Importe" name="amount" value={formatAmount(debt.amount)} disabled />
            </div>
            <div className="col-md-4">
              <Field label="Estado" name="status" value={formatDebtStatus(debt.status)} disabled />
            </div>
          </div>
        )}
        <div className="section-actions">
          <BackButton to="/tesoreria/deudas" />
          {debt?.status === 'PENDING' && (
            <button type="button" className="btn btn-primary" onClick={() => setConfirming(true)}>
              Condonar
            </button>
          )}
        </div>
      </section>
      <ConfirmModal
        show={confirming}
        title="Condonar deuda"
        message={`La deuda del CUIT ${formatCuit(debt?.cuit)} pasa a condonada y conserva su importe de ${formatAmount(debt?.amount)}. ¿Querés condonarla?`}
        confirmLabel="Condonar"
        busy={busy}
        onConfirm={forgive}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}
