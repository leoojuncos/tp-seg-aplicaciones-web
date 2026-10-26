import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loading } from '../../components/common/index.js';
import { usePortalSession } from '../../hooks/usePortalSession.js';

// Salir del portal: descarta el contribuyente y vuelve a /ingresar. No hay nada que avisarle al back,
// que no guarda una sesion del portal.
export default function PortalLogoutView() {
  const { logout } = usePortalSession();
  const navigate = useNavigate();

  useEffect(() => {
    logout();
    navigate('/ingresar', { replace: true });
  }, [logout, navigate]);

  return <Loading visible />;
}
