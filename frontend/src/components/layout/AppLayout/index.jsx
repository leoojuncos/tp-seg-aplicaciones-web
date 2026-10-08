import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import ErrorBoundary from '../../common/ErrorBoundary/index.jsx';
import { usePermissions } from '../../../hooks/usePermissions.js';
import { useSession } from '../../../hooks/useSession.js';
import { Modules } from '../../../routes/modules.js';
import { PrivateRoutes } from '../../../routes/private.js';
import { PublicRoutes } from '../../../routes/public.js';
import './index.css';

const PUBLIC_MENU = PublicRoutes.filter((route) => route.menu);

// Encabezado y contenido de todas las pantallas. Sin sesion, el menu muestra las rutas publicas con
// menu: true. Con sesion, solo las opciones del modulo en el que se esta (sus rutas con menu: true,
// decorativas incluidas); los modulos no aparecen en el menu, se eligen en /welcome, al que lleva la
// marca. A la derecha, el usuario y Salir, o Ingresar. La variante minimal (login, welcome,
// unauthorized y 404) muestra solo la marca y la sesion. Props: minimal.
export default function AppLayout({ minimal = false }) {
  const { session, loading } = useSession();
  const { hasPermission } = usePermissions();
  const { pathname } = useLocation();

  const currentModule = Modules.find(
    (module) => pathname === module.prefix || pathname.startsWith(`${module.prefix}/`),
  );
  const moduleMenu =
    currentModule && hasPermission(currentModule.code)
      ? PrivateRoutes.filter((route) => route.menu && route.code === currentModule.code)
      : [];

  return (
    <>
      <header>
        <nav className="navbar navbar-expand app-navbar">
          <Link to={session ? '/welcome' : '/estado'} className="navbar-brand app-brand">
            <span className="material-symbols-outlined" aria-hidden="true">
              account_balance
            </span>
            SGM
          </Link>
          {!minimal && !loading && <NavMenu routes={session ? moduleMenu : PUBLIC_MENU} />}
          {!loading && <SessionArea session={session} minimal={minimal} />}
        </nav>
      </header>
      <main className="app-main">
        <ErrorBoundary key={pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>
    </>
  );
}

function NavMenu({ routes }) {
  return (
    <ul className="navbar-nav">
      {routes.map((route) => (
        <li key={route.path} className="nav-item">
          <NavLink to={route.path} className="nav-link">
            {route.title}
          </NavLink>
        </li>
      ))}
    </ul>
  );
}

function SessionArea({ session, minimal }) {
  if (session) {
    return (
      <div className="app-session ms-auto">
        <span className="material-symbols-outlined" aria-hidden="true">
          account_circle
        </span>
        <span>{session.username}</span>
        <Link to="/logout" className="nav-link app-logout">
          <span className="material-symbols-outlined" aria-hidden="true">
            power_settings_new
          </span>
          Salir
        </Link>
      </div>
    );
  }
  if (minimal) {
    return null;
  }
  return (
    <ul className="navbar-nav ms-auto">
      <li className="nav-item">
        <NavLink to="/login" className="nav-link">
          Ingresar
        </NavLink>
      </li>
    </ul>
  );
}
