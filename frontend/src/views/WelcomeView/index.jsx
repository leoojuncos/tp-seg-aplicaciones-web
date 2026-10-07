import { ModuleGrid } from '../../components/common/index.js';
import { usePermissions } from '../../hooks/usePermissions.js';
import { Modules } from '../../routes/modules.js';

// Selector de modulos despues del login: un boton por modulo habilitado en la sesion. Con uno solo va
// directo a ese modulo.
export default function WelcomeView() {
  const { hasPermission } = usePermissions();
  return (
    <ModuleGrid
      items={Modules.filter((module) => hasPermission(module.code))}
      redirectSingle
      emptyMessage="Tu usuario no tiene permisos para ingresar a ningún módulo."
    />
  );
}
