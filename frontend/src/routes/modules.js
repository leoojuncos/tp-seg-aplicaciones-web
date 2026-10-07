// Modulos del SGM: el permiso de la sesion que los habilita (permissions.code), el prefijo de sus
// rutas, el icono (un Material Symbol) y la pantalla por la que se entra, que agrega el ticket de cada
// modulo. Los usan el selector de /welcome y el encabezado, que por el prefijo sabe en que modulo se
// esta y muestra solo sus opciones. Ingresos Publicos y Contaduria son decorativos: aparecen segun el
// permiso, como los demas, y todas sus rutas son placeholder en routes/private.js.
export const Modules = [
  {
    code: 'ADMINISTRACION',
    prefix: '/administracion',
    title: 'Administración',
    icon: 'manage_accounts',
    path: '/administracion/usuarios',
  },
  {
    code: 'INGRESOS_PUBLICOS',
    prefix: '/ingresos-publicos',
    title: 'Ingresos Públicos',
    icon: 'request_quote',
    path: '/ingresos-publicos/tributos',
  },
  {
    code: 'TESORERIA',
    prefix: '/tesoreria',
    title: 'Tesorería',
    icon: 'account_balance_wallet',
    path: '/tesoreria/deudas',
  },
  {
    code: 'CONTADURIA',
    prefix: '/contaduria',
    title: 'Contaduría',
    icon: 'calculate',
    path: '/contaduria/asientos',
  },
  {
    code: 'AUDITORIA',
    prefix: '/auditoria',
    title: 'Auditoría',
    icon: 'policy',
    path: '/auditoria/eventos',
  },
];
