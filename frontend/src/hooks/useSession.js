import { useContext } from 'react';
import { SessionContext } from '../context/SessionProvider.jsx';

// Sesion del SGM: { session, loading, refresh, clear }. session es { username, role, permissions } o
// null; loading es true hasta la primera respuesta; refresh la vuelve a pedir (despues del login) y
// clear la descarta (despues del logout).
export function useSession() {
  return useContext(SessionContext);
}
