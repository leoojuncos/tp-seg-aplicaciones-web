// Formatos de pantalla. formatCuit: 20123456789 -> 20-12345678-9 (si no tiene 11 digitos, lo deja como
// viene). formatAmount: importe con separador de miles y dos decimales, sin signo (15.000,00).
// formatDebtStatus: el estado de una deuda en castellano.
const AMOUNT = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const DATE_TIME = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  timeZone: 'America/Argentina/Buenos_Aires',
});

const DEBT_STATUS = { PENDING: 'Pendiente', FORGIVEN: 'Condonada' };

// Los mismos tipos que Auditoria asigna a cada modulo (AuditEventService).
const EVENT_TYPE = {
  DEBT_CREATED: 'Alta de deuda',
  DEBT_FORGIVEN: 'Condonación de deuda',
  PAYMENT_SLIP_ISSUED: 'Emisión de boleta de pago',
  TAXPAYER_REGISTERED: 'Alta de contribuyente',
  TAX_RETURN_FILED: 'Presentación de DDJJ',
  TAX_ADDRESS_CHANGED: 'Cambio de domicilio fiscal',
  SUPPLIER_INVOICE_RECORDED: 'Registro de factura de proveedor',
  PAYMENT_ORDER_ISSUED: 'Emisión de orden de pago',
  USER_CREATED: 'Alta de usuario',
  PERMISSION_GRANTED: 'Asignación de permiso',
  EVENTS_EXPORTED: 'Exportación de eventos',
};

export function formatCuit(cuit) {
  return /^\d{11}$/.test(cuit ?? '') ? `${cuit.slice(0, 2)}-${cuit.slice(2, 10)}-${cuit.slice(10)}` : (cuit ?? '');
}

export function formatAmount(amount) {
  return amount == null ? '' : AMOUNT.format(amount);
}

export function formatDebtStatus(status) {
  return DEBT_STATUS[status] ?? status;
}

// formatRole: ADMINISTRADOR -> Administrador (capitaliza, sin mapa fijo). Los codigos de permiso no
// se formatean: se muestran tal cual (ADMINISTRACION, TESORERIA, AUDITORIA).
export function formatRole(role) {
  return role ? role.charAt(0) + role.slice(1).toLowerCase() : '';
}

// formatDateTime: un instante ISO-8601 -> fecha y hora argentina (12/10/2026 18:30), sea cual sea la
// zona del navegador: es la zona en que Auditoria toma el dia para el filtro por fechas. Vacio si no viene.
export function formatDateTime(iso) {
  return iso ? DATE_TIME.format(new Date(iso)) : '';
}

// formatEventType: el tipo de un evento de auditoria en castellano.
export function formatEventType(type) {
  return EVENT_TYPE[type] ?? type;
}
