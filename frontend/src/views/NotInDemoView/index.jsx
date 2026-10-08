import { BackButton } from '../../components/common/index.js';

// Destino de las opciones decorativas del menu (placeholder: true en routes/private.js).
export default function NotInDemoView() {
  return (
    <section className="generic-page">
      <span className="material-symbols-outlined generic-page-icon" aria-hidden="true">
        construction
      </span>
      <h2>Sección fuera de la demo</h2>
      <p>Esta sección no forma parte de la demo: está en el menú para que la navegación del sistema se vea completa.</p>
      <div className="generic-page-actions">
        <BackButton />
      </div>
    </section>
  );
}
