import { LoginView, PageNotFoundView, StatusView, UnauthorizedView, VepView } from '../views/index.js';

// Rutas sin sesion. Las publicas de negocio, como /vep, tambien van aca.
export const PublicRoutes = [
  { path: '/login', title: 'Ingresar', component: LoginView },
  { path: '/vep', title: 'Consulta de deuda', component: VepView },
  { path: '/estado', title: 'Estado de los servicios', component: StatusView },
  { path: '/unauthorized', title: 'Sesión vencida', component: UnauthorizedView },
  { path: '*', title: 'Página no encontrada', component: PageNotFoundView },
];
