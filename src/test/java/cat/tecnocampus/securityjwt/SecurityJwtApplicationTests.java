package cat.tecnocampus.securityjwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "JWT_SECRET=dGVzdC1vbmx5LWtleS10ZXN0LW9ubHkta2V5LXRlc3Qtb25seS1rZXktdGVzdC1vbmx5LWtleS0xMjM0NTY3OA==")
@AutoConfigureMockMvc
class SecurityJwtApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void helloWorldIsPublic() throws Exception {
        mockMvc.perform(get("/helloWorld")).andExpect(status().isOk());
    }

    @Test
    void adminEndpointRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/helloAdmin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/helloAdmin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/helloUser")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginIssuesTokenAndTokenGrantsAccess() throws Exception {
        String body = mockMvc.perform(post("/token").with(httpBasic("maria", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"access_token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/helloUser").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void badCredentialsReturn401WithoutBasicChallenge() throws Exception {
        // No "WWW-Authenticate: Basic" header, so a browser does not open its own login dialog
        mockMvc.perform(post("/token").with(httpBasic("maria", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void browserApplicationCanCallTokenEndpoint() throws Exception {
        mockMvc.perform(options("/token")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(options("/token")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointsWithoutRuleAreDenied() throws Exception {
        // /moderator/** has no rule yet (TODO): denyAll() applies, even to ADMIN
        mockMvc.perform(get("/moderator/aaa")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/moderator/aaa").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void helloUsernameUsesMethodSecurity() throws Exception {
        var josep = jwt().jwt(token -> token.subject("josep")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
        mockMvc.perform(get("/hello/josep").with(josep)).andExpect(status().isOk());
        mockMvc.perform(get("/hello/maria").with(josep)).andExpect(status().isForbidden());
        var admin = jwt().jwt(token -> token.subject("jordi")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockMvc.perform(get("/hello/maria").with(admin)).andExpect(status().isOk());
    }
}
