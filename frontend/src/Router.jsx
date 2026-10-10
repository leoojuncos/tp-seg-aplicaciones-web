import { useEffect } from 'react';
import { matchPath, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Loading, PrivateRoute, PublicRoute } from './components/common/index.js';
import AppLayout from './components/layout/AppLayout/index.jsx';
import { useSession } from './hooks/useSession.js';
import { PrivateRoutes } from './routes/private.js';
import { PublicRoutes } from './routes/public.js';
import { NotInDemoView } from './views/index.js';

const ALL_ROUTES = [...PublicRoutes, ...PrivateRoutes];

const isMinimal = (route) => route.layout === 'minimal';

// Cada entrada de routes/ como <Route>: las opciones decorativas (placeholder) se resuelven a
// NotInDemoView, las que son solo para quien no tiene sesion (guestOnly) van dentro de PublicRoute, y
// las privadas, dentro de PrivateRoute con su permiso (code).
function renderRoutes(routes, { requiresSession = false } = {}) {
  return routes.map((route) => {
    const Component = route.placeholder ? NotInDemoView : route.component;
    let element = <Component />;
    if (route.guestOnly) {
      element = <PublicRoute>{element}</PublicRoute>;
    }
    if (requiresSession) {
      element = <PrivateRoute code={route.code}>{element}</PrivateRoute>;
    }
    return <Route key={route.path} path={route.path} element={element} />;
  });
}

// Arma el arbol de rutas desde routes/public.js y routes/private.js: cada ruta dentro del layout que
// pide (el completo o el minimal) y las privadas detras de PrivateRoute, que exige la sesion y el
// permiso de la ruta.
export default function Router() {
  const { pathname } = useLocation();
  const { session, loading } = useSession();

  useEffect(() => {
    const route =
      ALL_ROUTES.find((candidate) => candidate.path !== '*' && matchPath(candidate.path, pathname)) ??
      ALL_ROUTES.find((candidate) => candidate.path === '*');
    document.title = route ? `SGM · ${route.title}` : 'SGM';
  }, [pathname]);

  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route
          index
          element={loading ? <Loading visible /> : <Navigate to={session ? '/welcome' : '/login'} replace />}
        />
        {renderRoutes(PublicRoutes.filter((route) => !isMinimal(route)))}
        {renderRoutes(PrivateRoutes.filter((route) => !isMinimal(route)), { requiresSession: true })}
      </Route>
      <Route element={<AppLayout minimal />}>
        {renderRoutes(PublicRoutes.filter(isMinimal))}
        {renderRoutes(PrivateRoutes.filter(isMinimal), { requiresSession: true })}
      </Route>
    </Routes>
  );
}
