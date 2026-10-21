package cat.tecnocampus.securityjwt.security.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  public AuthenticationController(AuthenticationService service) {
    this.authenticationService = service;
  }

  // Spring Security has already checked the Basic credentials when this method runs
  @PostMapping("/token")
  public AuthenticationResponse token(Authentication authentication) {
    return authenticationService.issueToken(authentication);
  }
}
