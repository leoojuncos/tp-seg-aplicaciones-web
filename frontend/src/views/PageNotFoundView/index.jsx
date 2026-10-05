import { Link } from 'react-router-dom';

export default function PageNotFoundView() {
  return (
    <section>
      <h1>Página no encontrada</h1>
      <p>
        <Link to="/estado">Volver al estado de los servicios</Link>
      </p>
    </section>
  );
}
