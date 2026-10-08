import { useEffect } from 'react';
import { matchPath, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { PrivateRoute, PublicRoute } from './components/common/index.js';
import AppLayout from './components/layout/AppLayout/index.jsx';
import { PrivateRoutes } from './routes/private.js';
import { PublicRoutes } from './routes/public.js';
import { NotInDemoView } from './views/index.js';

const ALL_ROUTES = [...PublicRoutes, ...PrivateRoutes];

const isMinimal = (route) => route.layout === 'minimal';

// Cada entrada de routes/ como <Route>: las opciones decorativas (placeholder) se resuelven a
// NotInDemoView, y las que son solo para quien no tiene sesion (guestOnly) van dentro de PublicRoute.
function renderRoutes(routes) {
  return routes.map((route) => {
    const Component = route.placeholder ? NotInDemoView : route.component;
    const element = route.guestOnly ? (
      <PublicRoute>
        <Component />
      </PublicRoute>
    ) : (
      <Component />
    );
    return <Route key={route.path} path={route.path} element={element} />;
  });
}

// Arma el arbol de rutas desde routes/public.js y routes/private.js: cada ruta dentro del layout que
// pide (el completo o el minimal) y las privadas detras de PrivateRoute. El gating por permiso (code)
// lo agrega TPS-15.
export default function Router() {
  const { pathname } = useLocation();

  useEffect(() => {
    const route =
      ALL_ROUTES.find((candidate) => candidate.path !== '*' && matchPath(candidate.path, pathname)) ??
      ALL_ROUTES.find((candidate) => candidate.path === '*');
    document.title = route ? `SGM · ${route.title}` : 'SGM';
  }, [pathname]);

  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/estado" replace />} />
        {renderRoutes(PublicRoutes.filter((route) => !isMinimal(route)))}
        <Route
          element={
            <PrivateRoute>
              <Outlet />
            </PrivateRoute>
          }
        >
          {renderRoutes(PrivateRoutes.filter((route) => !isMinimal(route)))}
        </Route>
      </Route>
      <Route element={<AppLayout minimal />}>
        {renderRoutes(PublicRoutes.filter(isMinimal))}
        <Route
          element={
            <PrivateRoute>
              <Outlet />
            </PrivateRoute>
          }
        >
          {renderRoutes(PrivateRoutes.filter(isMinimal))}
        </Route>
      </Route>
    </Routes>
  );
}
