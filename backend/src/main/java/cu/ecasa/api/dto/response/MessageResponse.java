package cu.ecasa.api.dto.response;

/**
 * DTO de respuesta simple con un mensaje (para logout, delete, etc.).
 */
public record MessageResponse(String message) {
}