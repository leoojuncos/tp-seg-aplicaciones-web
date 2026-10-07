import { Navigate, useLocation } from 'react-router-dom';
import { useSession } from '../../../hooks/useSession.js';
import Loading from '../Loading/index.jsx';

// Envuelve las rutas que son solo para quien no tiene sesion (guestOnly, como /login): con sesion
// manda a donde se queria ir (state.from, que deja PrivateRoute) o a /welcome. Props: children.
export default function PublicRoute({ children }) {
  const { session, loading } = useSession();
  const location = useLocation();
  if (loading) {
    return <Loading visible />;
  }
  if (session) {
    return <Navigate to={location.state?.from ?? '/welcome'} replace />;
  }
  return children;
}
