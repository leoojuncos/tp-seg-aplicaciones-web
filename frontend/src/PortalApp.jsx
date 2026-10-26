import { useEffect } from 'react';
import { BrowserRouter, matchPath, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import PortalLayout from './components/layout/PortalLayout/index.jsx';
import { PortalSessionProvider } from './context/PortalSessionProvider.jsx';
import { usePortalSession } from './hooks/usePortalSession.js';
import { PortalRoutes } from './routes/portal.js';
import NotInDemoView from './views/NotInDemoView/index.jsx';
import PageNotFoundView from './views/PageNotFoundView/index.jsx';

const SITE = 'Portal del Contribuyente';

// App del portal del contribuyente, la que sirve vite.portal.config.js en su propio puerto: un sitio
// aparte del SGM, sin su sesion ni sus pantallas (por eso las vistas se importan por su ruta y no desde
// views/index.js). Las rutas de routes/portal.js van dentro de PortalLayout: las guestOnly (el ingreso)
// en su variante sin menus, y el resto detras de RequireTaxpayer. Las decorativas (placeholder) se
// resuelven a NotInDemoView, con Volver al inicio del portal.
export default function PortalApp() {
  return (
    <BrowserRouter>
      <PortalSessionProvider>
        <PortalRouter />
      </PortalSessionProvider>
    </BrowserRouter>
  );
}

function PortalRouter() {
  const { pathname } = useLocation();

  useEffect(() => {
    const route = PortalRoutes.find((candidate) => matchPath(candidate.path, pathname));
    document.title = `${SITE} · ${route ? route.title : 'Página no encontrada'}`;
  }, [pathname]);

  return (
    <Routes>
      <Route element={<PortalLayout minimal />}>
        {renderRoutes(PortalRoutes.filter((route) => route.guestOnly), GuestOnly)}
      </Route>
      <Route
        element={
          <RequireTaxpayer>
            <PortalLayout />
          </RequireTaxpayer>
        }
      >
        {renderRoutes(PortalRoutes.filter((route) => !route.guestOnly))}
        <Route path="*" element={<PageNotFoundView />} />
      </Route>
    </Routes>
  );
}

function renderRoutes(routes, Wrapper) {
  return routes.map((route) => {
    const Component = route.component;
    let element = route.placeholder ? <NotInDemoView backTo="/" /> : <Component />;
    if (Wrapper) {
      element = <Wrapper>{element}</Wrapper>;
    }
    return <Route key={route.path} path={route.path} element={element} />;
  });
}

// Sin haber ingresado, todo lleva a /ingresar.
function RequireTaxpayer({ children }) {
  const { taxpayer } = usePortalSession();
  return taxpayer ? children : <Navigate to="/ingresar" replace />;
}

// El ingreso es solo para quien no entro: con el contribuyente ya cargado, va al inicio.
function GuestOnly({ children }) {
  const { taxpayer } = usePortalSession();
  return taxpayer ? <Navigate to="/" replace /> : children;
}
