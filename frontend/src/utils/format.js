// Formatos de pantalla. formatCuit: 20123456789 -> 20-12345678-9 (si no tiene 11 digitos, lo deja como
// viene). formatAmount: importe con separador de miles y dos decimales, sin signo (15.000,00).
// formatDebtStatus: el estado de una deuda en castellano.
const AMOUNT = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const DEBT_STATUS = { PENDING: 'Pendiente', FORGIVEN: 'Condonada' };

export function formatCuit(cuit) {
  return /^\d{11}$/.test(cuit ?? '') ? `${cuit.slice(0, 2)}-${cuit.slice(2, 10)}-${cuit.slice(10)}` : (cuit ?? '');
}

export function formatAmount(amount) {
  return amount == null ? '' : AMOUNT.format(amount);
}

export function formatDebtStatus(status) {
  return DEBT_STATUS[status] ?? status;
}
