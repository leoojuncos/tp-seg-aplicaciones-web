import { useEffect } from 'react';
import { matchPath, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import PrivateRoute from './components/common/PrivateRoute.jsx';
import AppLayout from './components/layout/AppLayout.jsx';
import { PrivateRoutes } from './routes/private.js';
import { PublicRoutes } from './routes/public.js';

const ALL_ROUTES = [...PublicRoutes, ...PrivateRoutes];

// Arma el arbol de rutas desde routes/public.js y routes/private.js. Las privadas van detras de
// PrivateRoute; el filtrado por permiso (code) lo agrega TPS-15.
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
        {PublicRoutes.map(({ path, component: Component }) => (
          <Route key={path} path={path} element={<Component />} />
        ))}
        <Route
          element={
            <PrivateRoute>
              <Outlet />
            </PrivateRoute>
          }
        >
          {PrivateRoutes.map(({ path, component: Component }) => (
            <Route key={path} path={path} element={<Component />} />
          ))}
        </Route>
      </Route>
    </Routes>
  );
}
