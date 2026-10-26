import { LoginView, LogoutView, PageNotFoundView, StatusView, UnauthorizedView } from '../views/index.js';

// Rutas del SGM sin sesion. El portal del contribuyente no va aca: es otra app (PortalApp), con sus
// rutas en portal.js. Banderas: layout: 'minimal' (encabezado con solo la marca y la sesion), menu: true
// (aparece en el menu del encabezado cuando no hay sesion) y guestOnly: true (solo para quien no tiene
// sesion: con sesion, PublicRoute redirige).
export const PublicRoutes = [
  { path: '/login', title: 'Ingresar', component: LoginView, layout: 'minimal', guestOnly: true },
  { path: '/logout', title: 'Salir', component: LogoutView, layout: 'minimal' },
  { path: '/estado', title: 'Estado', component: StatusView, menu: true },
  { path: '/unauthorized', title: 'Sin acceso', component: UnauthorizedView, layout: 'minimal' },
  { path: '*', title: 'Página no encontrada', component: PageNotFoundView, layout: 'minimal' },
];
