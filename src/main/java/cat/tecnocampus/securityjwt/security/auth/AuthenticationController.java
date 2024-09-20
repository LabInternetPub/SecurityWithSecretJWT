package cat.tecnocampus.securityjwt.security.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  @Value("${application.security.jwt.token-prefix}")
  private String tokenPrefix;


  public AuthenticationController(AuthenticationService service) {
    this.authenticationService = service;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
    AuthenticationResponse response = authenticationService.authenticate(request);

    //sending the token in the header and the body
    return ResponseEntity.ok()
            .header("Authorization", tokenPrefix + response.getAccessToken())
            .body(response);
  }
}