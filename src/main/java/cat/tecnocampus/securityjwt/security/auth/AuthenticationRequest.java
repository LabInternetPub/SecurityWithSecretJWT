package cat.tecnocampus.securityjwt.security.auth;

public record AuthenticationRequest (String username, String password) {
}
