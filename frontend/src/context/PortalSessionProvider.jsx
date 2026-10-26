import { createContext, useCallback, useMemo, useState } from 'react';

export const PortalSessionContext = createContext(null);

const STORAGE_KEY = 'portal_taxpayer';

// Contribuyente que entro al portal: { cuit, name }, lo que devuelve POST /api/vep/auth/login. El back
// no guarda una sesion del portal (el login solo valida la clave), asi que se guarda en sessionStorage:
// sobrevive a un F5 y se pierde al cerrar la pestana.
export function PortalSessionProvider({ children }) {
  const [taxpayer, setTaxpayer] = useState(readStored);

  const login = useCallback((value) => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    } catch {
      // Sin sessionStorage (modo privado estricto), el ingreso dura hasta recargar.
    }
    setTaxpayer(value);
  }, []);

  const logout = useCallback(() => {
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nada que borrar.
    }
    setTaxpayer(null);
  }, []);

  const value = useMemo(() => ({ taxpayer, login, logout }), [taxpayer, login, logout]);
  return <PortalSessionContext.Provider value={value}>{children}</PortalSessionContext.Provider>;
}

function readStored() {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}
