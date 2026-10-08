import { BackButton } from '../../components/common/index.js';

export default function PageNotFoundView() {
  return (
    <section className="generic-page">
      <span className="material-symbols-outlined generic-page-icon" aria-hidden="true">
        travel_explore
      </span>
      <h2>Página no encontrada</h2>
      <p>Revisá que la dirección sea correcta.</p>
      <div className="generic-page-actions">
        <BackButton />
      </div>
    </section>
  );
}
