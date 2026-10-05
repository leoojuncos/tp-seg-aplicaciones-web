import { WelcomeView } from '../views/index.js';

// Rutas que exigen sesion del SGM. `code` es el permiso que habilita la ruta (permissions.code:
// ADMINISTRACION, TESORERIA o AUDITORIA); null = alcanza con tener sesion. El filtrado por permiso
// lo agrega TPS-15. Cada modulo suma las suyas con su prefijo (/administracion, /tesoreria,
// /auditoria), en castellano: listado en plural (/tesoreria/deudas) y detalle en singular con
// parametro (/tesoreria/deuda/:cuit).
export const PrivateRoutes = [
  { path: '/welcome', title: 'Módulos', component: WelcomeView, code: null },
];
