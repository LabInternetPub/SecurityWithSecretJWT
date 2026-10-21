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
    void badCredentialsReturnProblemDetail() throws Exception {
        mockMvc.perform(post("/token").with(httpBasic("maria", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}
