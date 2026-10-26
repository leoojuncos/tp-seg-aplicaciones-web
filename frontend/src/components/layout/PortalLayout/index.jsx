import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import ErrorBoundary from '../../common/ErrorBoundary/index.jsx';
import { usePortalSession } from '../../../hooks/usePortalSession.js';
import { PortalRoutes } from '../../../routes/portal.js';
import './index.css';

const MENU = PortalRoutes.filter((route) => route.menu);
const USER_MENU = PortalRoutes.filter((route) => route.userMenu);

const ADDRESS = 'Av. San Martín 1250';
const PHONE = '0800-555-2026';
const OPENING_HOURS = 'Lunes a viernes de 8 a 14 h';

const USEFUL_PHONES = [
  ['Bomberos', '100'],
  ['Policía', '911'],
  ['Emergencia médica', '107'],
  ['Defensa Civil', '103'],
];

// Encabezado y pie del portal del contribuyente (PortalApp), un sitio aparte del SGM: no lleva su marca,
// su menu ni su sesion. Arriba, una barra con los datos de contacto del municipio y el contribuyente, con
// su menu (rutas con userMenu: true); debajo, la marca del portal y su menu (rutas con menu: true); al
// pie, contacto y telefonos utiles. La variante minimal (el ingreso) no muestra los dos menus. Los
// colores del portal son propios y estan en este index.css. Props: minimal.
export default function PortalLayout({ minimal = false }) {
  const { pathname } = useLocation();

  return (
    <div className="portal">
      <header className="portal-topbar">
        <ul className="portal-contact">
          <li>
            <Icon name="location_on" />
            {ADDRESS}
          </li>
          <li>
            <Icon name="call" />
            {PHONE}
          </li>
          <li>
            <Icon name="schedule" />
            {OPENING_HOURS}
          </li>
        </ul>
        {!minimal && <UserMenu />}
      </header>
      <nav className="navbar navbar-expand portal-navbar">
        <Link to="/" className="navbar-brand portal-brand">
          <Icon name="location_city" />
          <span>
            Portal del Contribuyente
            <small>Municipalidad</small>
          </span>
        </Link>
        {!minimal && (
          <ul className="navbar-nav ms-auto">
            {MENU.map((route) => (
              <li key={route.path} className="nav-item">
                <NavLink to={route.path} end className="nav-link">
                  {route.title}
                </NavLink>
              </li>
            ))}
          </ul>
        )}
      </nav>
      <main className="portal-main">
        <ErrorBoundary key={pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>
      <footer className="portal-footer">
        <div className="portal-footer-content">
          <section>
            <h3>
              <Icon name="chevron_right" />
              Contacto
            </h3>
            <p>{ADDRESS}</p>
            <p>{PHONE}</p>
            <p>{OPENING_HOURS}</p>
          </section>
          <section>
            <h3>
              <Icon name="chevron_right" />
              Teléfonos útiles
            </h3>
            {USEFUL_PHONES.map(([name, number]) => (
              <p key={name}>
                {name}: {number}
              </p>
            ))}
          </section>
        </div>
      </footer>
    </div>
  );
}

// Nombre del contribuyente con su menu desplegable. Bootstrap trae solo el CSS, asi que se abre con
// estado propio y se cierra al elegir una opcion o al hacer clic afuera.
function UserMenu() {
  const { taxpayer } = usePortalSession();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const closeOnOutsideClick = (event) => {
      if (!ref.current?.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', closeOnOutsideClick);
    return () => document.removeEventListener('mousedown', closeOnOutsideClick);
  }, [open]);

  return (
    <div className="dropdown portal-user" ref={ref}>
      <button
        type="button"
        className="btn dropdown-toggle"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        {taxpayer.name}
      </button>
      {/* data-bs-popper: sin el JS de Bootstrap, es lo que hace que su CSS ubique el menu debajo del boton. */}
      <ul className={`dropdown-menu dropdown-menu-end${open ? ' show' : ''}`} data-bs-popper="static">
        {USER_MENU.map((route) => (
          <li key={route.path}>
            <Link to={route.path} className="dropdown-item" onClick={() => setOpen(false)}>
              {route.title}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Icon({ name }) {
  return (
    <span className="material-symbols-outlined" aria-hidden="true">
      {name}
    </span>
  );
}
