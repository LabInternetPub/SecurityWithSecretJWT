package cat.tecnocampus.securityjwt.security.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {
  private final JwtEncoder jwtEncoder;
  private final Duration expiration;

  public AuthenticationService(JwtEncoder jwtEncoder,
                               @Value("${application.security.jwt.expiration}") Duration expiration) {
    this.jwtEncoder = jwtEncoder;
    this.expiration = expiration;
  }

  public AuthenticationResponse issueToken(Authentication authentication) {
    return new AuthenticationResponse(generateToken(authentication), "Bearer", expiration.toSeconds());
  }

  public String generateToken(Authentication authentication) {
    Instant now = Instant.now();
    List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))   // Spring Security 7 also adds FACTOR_PASSWORD
            .toList();

    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(SecurityConfigurationBeans.ISSUER)
            .audience(List.of(SecurityConfigurationBeans.AUDIENCE))
            .subject(authentication.getName())
            .issuedAt(now)
            .expiresAt(now.plus(expiration))
            .id(UUID.randomUUID().toString())
            .claim("client_id", "securityjwt-login")
            .claim("roles", roles)
            .build();

    var header = JwsHeader.with(MacAlgorithm.HS512).type("at+jwt").build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
