import { NavLink, Outlet } from 'react-router-dom';

// Encabezado y contenido. El menu por modulos, armado desde PrivateRoutes, lo agrega TPS-15.
export default function AppLayout() {
  return (
    <>
      <header className="app-header">
        <strong>SGM</strong>
        <nav>
          <NavLink to="/estado">Estado</NavLink>
          <NavLink to="/vep">Consulta de deuda</NavLink>
          <NavLink to="/login">Ingresar</NavLink>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </>
  );
}
