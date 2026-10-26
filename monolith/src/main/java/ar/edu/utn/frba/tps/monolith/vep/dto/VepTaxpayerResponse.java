package ar.edu.utn.frba.tps.monolith.vep.dto;

/** El contribuyente que entro al portal: su CUIT y su nombre, sin el hash de la clave. */
public record VepTaxpayerResponse(String cuit, String name) {
}
