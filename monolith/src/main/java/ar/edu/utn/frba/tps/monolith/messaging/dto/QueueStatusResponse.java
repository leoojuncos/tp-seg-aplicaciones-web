package ar.edu.utn.frba.tps.monolith.messaging.dto;

public record QueueStatusResponse(String queue, int messages, int consumers) {
}
