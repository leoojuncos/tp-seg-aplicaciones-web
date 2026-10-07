import { useCallback } from 'react';
import { useSession } from './useSession.js';

const NO_PERMISSIONS = [];

// Permisos de la sesion: { permissions, ready, hasPermission }. permissions son los codigos de
// permissions.code (los code de routes/modules.js); ready es false mientras carga la sesion.
export function usePermissions() {
  const { session, loading } = useSession();
  const permissions = session?.permissions ?? NO_PERMISSIONS;
  const hasPermission = useCallback((code) => permissions.includes(code), [permissions]);
  return { permissions, ready: !loading, hasPermission };
}
