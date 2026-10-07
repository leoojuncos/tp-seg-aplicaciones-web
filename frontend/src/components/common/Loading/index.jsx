import './index.css';

// Overlay con spinner sobre toda la pagina mientras corre algo. Aparece con 200 ms de demora, asi una
// respuesta rapida no lo hace parpadear. Props: visible.
export default function Loading({ visible = false }) {
  if (!visible) {
    return null;
  }
  return (
    <div className="sgm-loading" role="status">
      <div className="spinner-border" aria-hidden="true" />
      <span className="visually-hidden">Cargando…</span>
    </div>
  );
}
