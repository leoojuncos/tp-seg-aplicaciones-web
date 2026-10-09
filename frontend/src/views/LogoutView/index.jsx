import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loading } from '../../components/common/index.js';
import { postJson } from '../../api/http.js';
import { useSession } from '../../hooks/useSession.js';

// Ruta publica (no guestOnly: tiene que funcionar con sesion activa, que es el caso normal al
// apretar "Salir"). Vence la cookie, limpia el contexto y vuelve a /login.
export default function LogoutView() {
  const { clear } = useSession();
  const navigate = useNavigate();

  useEffect(() => {
    postJson('/api/auth/logout', undefined, { authRedirect: false }).finally(() => {
      clear();
      navigate('/login', { replace: true });
    });
  }, [clear, navigate]);

  return <Loading visible />;
}
