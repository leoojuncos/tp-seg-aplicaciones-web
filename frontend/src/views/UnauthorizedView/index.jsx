import { Link } from 'react-router-dom';

export default function UnauthorizedView() {
  return (
    <section>
      <h1>Sesión vencida</h1>
      <p>
        La sesión venció o el usuario no tiene acceso a esta sección. <Link to="/login">Volver a ingresar</Link>.
      </p>
    </section>
  );
}
