import { LoginView, LogoutView, PageNotFoundView, StatusView, UnauthorizedView, VepView } from '../views/index.js';

// Rutas sin sesion. Las publicas de negocio, como /vep, tambien van aca. Banderas: layout: 'minimal'
// (encabezado con solo la marca y la sesion), menu: true (aparece en el menu del encabezado cuando no
// hay sesion) y guestOnly: true (solo para quien no tiene sesion: con sesion, PublicRoute redirige).
export const PublicRoutes = [
  { path: '/login', title: 'Ingresar', component: LoginView, layout: 'minimal', guestOnly: true },
  { path: '/logout', title: 'Salir', component: LogoutView, layout: 'minimal' },
  { path: '/estado', title: 'Estado', component: StatusView, menu: true },
  { path: '/vep', title: 'Consulta de deuda', component: VepView, menu: true },
  { path: '/unauthorized', title: 'Sin acceso', component: UnauthorizedView, layout: 'minimal' },
  { path: '*', title: 'Página no encontrada', component: PageNotFoundView, layout: 'minimal' },
];
