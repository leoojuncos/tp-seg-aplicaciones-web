import { Navigate, useLocation } from 'react-router-dom';
import { usePermissions } from '../../../hooks/usePermissions.js';
import { useSession } from '../../../hooks/useSession.js';
import Loading from '../Loading/index.jsx';

// Envuelve una ruta privada: mientras carga la sesion muestra Loading, sin sesion manda al login
// recordando a donde se queria ir (state.from), y si la ruta pide un permiso (code) que la sesion no
// tiene, manda a /unauthorized. Props: code (el permiso de la ruta; null o ausente = alcanza con la
// sesion) y children.
export default function PrivateRoute({ code = null, children }) {
  const { session, loading } = useSession();
  const { hasPermission } = usePermissions();
  const location = useLocation();
  if (loading) {
    return <Loading visible />;
  }
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (code && !hasPermission(code)) {
    return <Navigate to="/unauthorized" replace />;
  }
  return children;
}
