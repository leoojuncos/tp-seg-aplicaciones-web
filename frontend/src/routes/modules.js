// Modulos del SGM: el permiso de la sesion que los habilita (permissions.code), el prefijo de sus
// rutas, el icono (un Material Symbol) y la pantalla por la que se entra, que agrega el ticket de cada
// modulo. Los usan el selector de /welcome y el encabezado, que por el prefijo sabe en que modulo se
// esta y muestra solo sus opciones.
export const Modules = [
  {
    code: 'ADMINISTRACION',
    prefix: '/administracion',
    title: 'Administración',
    icon: 'manage_accounts',
    path: '/administracion/usuarios',
  },
  {
    code: 'TESORERIA',
    prefix: '/tesoreria',
    title: 'Tesorería',
    icon: 'account_balance_wallet',
    path: '/tesoreria/deudas',
  },
  {
    code: 'AUDITORIA',
    prefix: '/auditoria',
    title: 'Auditoría',
    icon: 'policy',
    path: '/auditoria/eventos',
  },
];
