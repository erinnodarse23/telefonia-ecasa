package cu.ecasa.api.security;

public record AuthenticatedUser(
        Integer id,
        String login,
        String rol,
        Integer uebId,
        String jti) {
}