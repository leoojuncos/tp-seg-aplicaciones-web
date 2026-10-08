import { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getJson, setAuthHandlers } from '../api/http.js';

export const SessionContext = createContext(null);

const SESSION_PATH = '/api/auth/session';

// Sesion del SGM: la expone el monolito en GET /api/auth/session (TPS-13). Cualquier respuesta que
// no sea 200 cuenta como sin sesion (401 sin cookie o invalida). Se lee al montar la app y despues
// del login (refresh); no en cada navegacion.
export function SessionProvider({ children }) {
  const [state, setState] = useState({ session: null, loading: true });
  const navigate = useNavigate();
  const location = useLocation();
  const locationRef = useRef(location);

  useEffect(() => {
    locationRef.current = location;
  }, [location]);

  const refresh = useCallback(async () => {
    try {
      // Sin authRedirect: que no haya sesion no es una sesion vencida, y las paginas publicas no
      // tienen que mandar al login.
      const response = await getJson(SESSION_PATH, { authRedirect: false });
      setState({ session: response.ok ? response.body : null, loading: false });
    } catch {
      setState({ session: null, loading: false });
    }
  }, []);

  const clear = useCallback(() => setState({ session: null, loading: false }), []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    setAuthHandlers({
      onUnauthorized: () => {
        clear();
        // Con varios pedidos en paralelo llegan varios 401: el primero ya mando al login, y los demas
        // no tienen que pisar el destino recordado con /login.
        if (locationRef.current.pathname !== '/login') {
          navigate('/login', { replace: true, state: { from: locationRef.current } });
        }
      },
      onForbidden: () => navigate('/unauthorized', { replace: true }),
    });
  }, [clear, navigate]);

  const value = useMemo(() => ({ ...state, refresh, clear }), [state, refresh, clear]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}
