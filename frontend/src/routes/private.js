import { DebtsView, DebtView, WelcomeView } from '../views/index.js';

// Rutas que exigen sesion del SGM. `code` es el permiso que habilita la ruta (permissions.code, el
// code de su modulo en routes/modules.js); null = alcanza con tener sesion. El gating por code lo
// agrega TPS-15. Cada modulo suma las suyas con su prefijo (/administracion, /tesoreria, /auditoria),
// en castellano: listado en plural (/tesoreria/deudas) y detalle en singular con parametro
// (/tesoreria/deuda/:id). Banderas: layout: 'minimal', menu: true (aparece en el menu del encabezado
// mientras se esta en su modulo) y placeholder: true (opcion decorativa, sin component: el Router la
// resuelve a NotInDemoView). Ver las de Tesoreria.
export const PrivateRoutes = [
  { path: '/welcome', title: 'Módulos', component: WelcomeView, code: null, layout: 'minimal' },

  // Tesoreria. Cajas, Recaudaciones y Rendiciones son decorativas.
  { path: '/tesoreria/deudas', title: 'Deudas', component: DebtsView, code: 'TESORERIA', menu: true },
  { path: '/tesoreria/deuda/:id', title: 'Deuda', component: DebtView, code: 'TESORERIA' },
  { path: '/tesoreria/cajas', title: 'Cajas', code: 'TESORERIA', menu: true, placeholder: true },
  { path: '/tesoreria/recaudaciones', title: 'Recaudaciones', code: 'TESORERIA', menu: true, placeholder: true },
  { path: '/tesoreria/rendiciones', title: 'Rendiciones', code: 'TESORERIA', menu: true, placeholder: true },

  // Modulos decorativos (ver routes/modules.js): solo menu, sin pantallas.
  { path: '/ingresos-publicos/tributos', title: 'Tributos', code: 'INGRESOS_PUBLICOS', menu: true, placeholder: true },
  { path: '/ingresos-publicos/emision', title: 'Emisión', code: 'INGRESOS_PUBLICOS', menu: true, placeholder: true },
  { path: '/ingresos-publicos/financiacion', title: 'Financiación', code: 'INGRESOS_PUBLICOS', menu: true, placeholder: true },
  { path: '/contaduria/asientos', title: 'Asientos', code: 'CONTADURIA', menu: true, placeholder: true },
  { path: '/contaduria/plan-de-cuentas', title: 'Plan de cuentas', code: 'CONTADURIA', menu: true, placeholder: true },
  { path: '/contaduria/balances', title: 'Balances', code: 'CONTADURIA', menu: true, placeholder: true },
];
