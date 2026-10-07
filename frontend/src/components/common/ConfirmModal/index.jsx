import Modal from '../Modal/index.jsx';

// Pide confirmacion antes de una accion. Props: show (true), title ('Confirmación'), message (que va a
// pasar, en una linea y terminando en una pregunta), confirmLabel ('Continuar'; tiene que responder la
// pregunta), cancelLabel ('Cancelar'), danger (la accion es destructiva: confirmar va en rojo), busy
// (deshabilita los botones mientras corre la accion), onConfirm y onCancel.
export default function ConfirmModal({
  show = true,
  title = 'Confirmación',
  message,
  confirmLabel = 'Continuar',
  cancelLabel = 'Cancelar',
  danger = false,
  busy = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal
      show={show}
      title={title}
      onClose={busy ? undefined : onCancel}
      footer={
        <>
          <button type="button" className="btn btn-outline-secondary" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`btn ${danger ? 'btn-outline-danger' : 'btn-primary'}`}
            onClick={onConfirm}
            disabled={busy}
          >
            {confirmLabel}
          </button>
        </>
      }
    >
      <p className="mb-0">{message}</p>
    </Modal>
  );
}
