import PortalLoginView from '../views/PortalLoginView/index.jsx';
import PortalLogoutView from '../views/PortalLogoutView/index.jsx';
import VepView from '../views/VepView/index.jsx';

// Rutas del portal del contribuyente (PortalApp, en su propio puerto). Funcionan el ingreso, el inicio
// con las deudas del contribuyente y Salir; el resto son opciones decorativas (placeholder: true, como en
// private.js) para que el portal se vea completo. Todas piden haber ingresado, salvo las guestOnly.
// Banderas: guestOnly: true (solo sin haber ingresado, con el layout sin menus), menu: true (aparece en
// el menu del portal) y userMenu: true (en el menu del contribuyente).
export const PortalRoutes = [
  { path: '/ingresar', title: 'Ingresar', component: PortalLoginView, guestOnly: true },
  { path: '/', title: 'Inicio', component: VepView, menu: true },
  { path: '/tramites', title: 'Trámites', placeholder: true, menu: true },
  { path: '/calendario-impositivo', title: 'Calendario impositivo', placeholder: true, menu: true },
  { path: '/preguntas-frecuentes', title: 'Preguntas frecuentes', placeholder: true, menu: true },
  { path: '/pagos', title: 'Pagar en línea', placeholder: true },
  { path: '/planes-de-pago', title: 'Planes de pago', placeholder: true },
  { path: '/libre-deuda', title: 'Libre deuda', placeholder: true },
  { path: '/boleta-electronica', title: 'Boleta electrónica', placeholder: true },
  { path: '/mis-datos', title: 'Editar mis datos', placeholder: true, userMenu: true },
  { path: '/contrasena', title: 'Modificar contraseña', placeholder: true, userMenu: true },
  { path: '/salir', title: 'Salir', component: PortalLogoutView, userMenu: true },
];
