package cat.tecnocampus.securityjwt;

import cat.tecnocampus.securityjwt.security.authentication.SecurityConfigurationBeans;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real tokens signed with the application's key and then broken in one way each.
// Every broken token must be rejected with 401 by the JwtDecoder (signature, algorithm, typ, iss, aud, exp).
@SpringBootTest(properties = "JWT_SECRET=dGVzdC1vbmx5LWtleS10ZXN0LW9ubHkta2V5LXRlc3Qtb25seS1rZXktdGVzdC1vbmx5LWtleS0xMjM0NTY3OA==")
@AutoConfigureMockMvc
class TokenValidationTests {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JwtEncoder jwtEncoder;
    @Autowired
    SecretKey jwtSigningKey;

    @Test
    void validTokenIsAccepted() throws Exception {
        callHelloAdmin(sign(jwtEncoder, claims -> {}, MacAlgorithm.HS512, "at+jwt")).andExpect(status().isOk());
    }

    @Test
    void tamperedPayloadIsRejected() throws Exception {
        // josep (USER) changes the payload to give himself ROLE_ADMIN and keeps the original signature
        String token = sign(jwtEncoder, claims -> claims.subject("josep").claim("roles", List.of("ROLE_USER")), MacAlgorithm.HS512, "at+jwt");
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"ROLE_USER\"", "\"ROLE_ADMIN\"");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        callHelloAdmin(tampered).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        SecretKey otherKey = new SecretKeySpec("another-key-another-key-another-key-another-key-another-key-1234".getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(otherKey).algorithm(MacAlgorithm.HS512).build();
        callHelloAdmin(sign(otherEncoder, claims -> {}, MacAlgorithm.HS512, "at+jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void otherAlgorithmWithTheRightKeyIsRejected() throws Exception {
        // Right key, but HS256 instead of HS512: the decoder accepts only HS512 (algorithm pinning)
        JwtEncoder hs256Encoder = NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
        callHelloAdmin(sign(hs256Encoder, claims -> {}, MacAlgorithm.HS256, "at+jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void unsignedTokenIsRejected() throws Exception {
        // alg "none": header and payload with an empty signature
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"at+jwt\"}");
        String payload = base64Url("{\"sub\":\"mallory\",\"roles\":[\"ROLE_ADMIN\"],\"iss\":\"" + SecurityConfigurationBeans.ISSUER
                + "\",\"aud\":\"" + SecurityConfigurationBeans.AUDIENCE + "\",\"client_id\":\"x\",\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}");
        callHelloAdmin(header + "." + payload + ".").andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        // Expired 2 minutes ago: beyond the 60 s of clock skew accepted by the decoder
        Instant now = Instant.now();
        String token = sign(jwtEncoder, claims -> claims.issuedAt(now.minusSeconds(1000)).expiresAt(now.minusSeconds(120)), MacAlgorithm.HS512, "at+jwt");
        callHelloAdmin(token).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerIsRejected() throws Exception {
        callHelloAdmin(sign(jwtEncoder, claims -> claims.issuer("https://evil.example"), MacAlgorithm.HS512, "at+jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongAudienceIsRejected() throws Exception {
        callHelloAdmin(sign(jwtEncoder, claims -> claims.audience(List.of("another-api")), MacAlgorithm.HS512, "at+jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongTypeIsRejected() throws Exception {
        // typ "JWT" instead of "at+jwt": it is not an access token (RFC 9068)
        callHelloAdmin(sign(jwtEncoder, claims -> {}, MacAlgorithm.HS512, "JWT")).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions callHelloAdmin(String token) throws Exception {
        return mockMvc.perform(get("/helloAdmin").header("Authorization", "Bearer " + token));
    }

    // A valid ADMIN token as issued by AuthenticationService; the consumer breaks one claim
    private static String sign(JwtEncoder encoder, Consumer<JwtClaimsSet.Builder> change, MacAlgorithm algorithm, String type) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(SecurityConfigurationBeans.ISSUER)
                .audience(List.of(SecurityConfigurationBeans.AUDIENCE))
                .subject("maria")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .id("test")
                .claim("client_id", "securityjwt-login")
                .claim("roles", List.of("ROLE_USER", "ROLE_ADMIN"));
        change.accept(claims);
        JwsHeader header = JwsHeader.with(algorithm).type(type).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
