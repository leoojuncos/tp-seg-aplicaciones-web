import { Link } from 'react-router-dom';
import { BackButton } from '../../components/common/index.js';

export default function UnauthorizedView() {
  return (
    <section className="generic-page">
      <span className="material-symbols-outlined generic-page-icon" aria-hidden="true">
        lock_clock
      </span>
      <h2>Sesión vencida o sin acceso</h2>
      <p>La sesión venció o tu usuario no tiene acceso a esta sección.</p>
      <div className="generic-page-actions">
        <BackButton />
        <Link to="/login" className="btn btn-primary">
          Ingresar
        </Link>
      </div>
    </section>
  );
}
