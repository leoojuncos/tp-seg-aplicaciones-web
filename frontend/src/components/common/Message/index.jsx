import './index.css';

const ICONS = { error: 'error', success: 'check_circle', neutral: 'info' };

// Mensaje inline y persistente, con icono y borde del color del tipo: error (rojo), success (verde) o
// neutral (gris). Props: type ('neutral') y children (el texto); sin texto no se muestra.
export default function Message({ type = 'neutral', children }) {
  if (!children) {
    return null;
  }
  return (
    <div className={`sgm-message sgm-message-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <span className="material-symbols-outlined sgm-message-icon" aria-hidden="true">
        {ICONS[type]}
      </span>
      <div>{children}</div>
    </div>
  );
}
