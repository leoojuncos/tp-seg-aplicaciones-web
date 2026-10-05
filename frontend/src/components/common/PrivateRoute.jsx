import { Navigate, useLocation } from 'react-router-dom';
import { useSession } from '../../hooks/useSession.js';

// Envuelve las rutas privadas: sin sesion manda al login y recuerda a donde se queria ir.
export default function PrivateRoute({ children }) {
  const session = useSession();
  const location = useLocation();
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return children;
}
