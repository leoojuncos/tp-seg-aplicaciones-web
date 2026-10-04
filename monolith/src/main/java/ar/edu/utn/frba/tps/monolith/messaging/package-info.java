/**
 * Modulo interno de mensajeria: unico punto de acceso a la cola desde el resto del monolito.
 * Tiene cuentas tecnicas y sesion propias, independientes de la sesion del SGM, y guarda los
 * eventos de auditoria pendientes, que son la fuente de verdad (la cola solo los transporta).
 */
package ar.edu.utn.frba.tps.monolith.messaging;
