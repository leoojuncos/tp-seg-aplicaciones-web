import { Navigate, useLocation } from 'react-router-dom';
import { useSession } from '../../../hooks/useSession.js';
import Loading from '../Loading/index.jsx';

// Envuelve las rutas privadas: mientras carga la sesion muestra Loading, y sin sesion manda al login
// recordando a donde se queria ir (state.from). Props: children.
export default function PrivateRoute({ children }) {
  const { session, loading } = useSession();
  const location = useLocation();
  if (loading) {
    return <Loading visible />;
  }
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return children;
}
