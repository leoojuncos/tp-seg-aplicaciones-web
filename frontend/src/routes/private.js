import { WelcomeView } from '../views/index.js';

// Rutas que exigen sesion del SGM. `code` es el permiso que habilita la ruta (permissions.code:
// ADMINISTRACION, TESORERIA o AUDITORIA); null = alcanza con tener sesion. El gating por code lo
// agrega TPS-15. Cada modulo suma las suyas con su prefijo (/administracion, /tesoreria, /auditoria),
// en castellano: listado en plural (/tesoreria/deudas) y detalle en singular con parametro
// (/tesoreria/deuda/:cuit). Banderas: layout: 'minimal', menu: true (aparece en el menu del encabezado
// mientras se esta en su modulo) y placeholder: true (opcion decorativa, sin component: el Router la
// resuelve a NotInDemoView). Por ejemplo:
//   { path: '/tesoreria/deudas', title: 'Deudas', component: DebtsView, code: 'TESORERIA', menu: true },
//   { path: '/tesoreria/recaudacion', title: 'Recaudación', code: 'TESORERIA', menu: true, placeholder: true },
export const PrivateRoutes = [
  { path: '/welcome', title: 'Módulos', component: WelcomeView, code: null, layout: 'minimal' },
];
