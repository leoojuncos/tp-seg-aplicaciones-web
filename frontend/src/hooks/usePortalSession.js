import { useContext } from 'react';
import { PortalSessionContext } from '../context/PortalSessionProvider.jsx';

// Contribuyente del portal: { taxpayer, login, logout }. taxpayer es { cuit, name } o null; login lo
// guarda despues del ingreso y logout lo descarta.
export function usePortalSession() {
  return useContext(PortalSessionContext);
}
